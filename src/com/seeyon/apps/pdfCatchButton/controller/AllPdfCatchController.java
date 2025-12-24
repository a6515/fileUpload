package com.seeyon.apps.pdfCatchButton.controller;
import com.google.gson.Gson; // 复用项目中已有的Gson
import com.seeyon.apps.pdfCatchButton.service.PdfCatchService;
import com.seeyon.apps.pdfCatchButton.vo.TaskProgress;
import com.seeyon.ctp.common.AppContext;
import com.seeyon.ctp.common.authenticate.domain.User;
import com.seeyon.ctp.common.controller.BaseController;
import com.seeyon.ctp.common.log.CtpLogFactory;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate; // 关键依赖
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Controller
public class AllPdfCatchController extends BaseController {
    // 注入配置环境
    @Value("${myRedis.password}")
    private String redisPassword;

    private static final Log LOGGER = CtpLogFactory.getLog(AllPdfCatchController.class);

    @Autowired
    private PdfCatchService pdfCatchService;

    // 【新增】注入 Redis 模板
    // 注意：需要在 spring xml 中配置过 StringRedisTemplate，或者致远环境已有相关 Bean
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Gson gson = new Gson();

    // 【删除】原来的内存缓存
    // private static final Map<String, TaskProgress> PROGRESS_CACHE = new
    // ConcurrentHashMap<>();

    // 定义 Redis Key 的前缀，方便管理
    private static final String REDIS_KEY_PREFIX = "pdf:catch:task:";
    // 定义任务过期时间 (例如 1 小时后自动删除)
    private static final long EXPIRE_HOURS = 1;

    @GetMapping(path = "/dj/startSync.do", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> startSync(
            @RequestParam(value = "startDate") String startDate,
            @RequestParam(value = "endDate") String endDate,
            @RequestParam(value = "company") String company,
            @RequestParam(value = "formId") String formId) {

        User user = AppContext.getCurrentUser();
        LOGGER.info("用户[" + user.getName() + "] 发起同步请求: " + company + ", " + startDate + "~" + endDate);

        Map<String, Object> response = new HashMap<>();
        String taskId = UUID.randomUUID().toString();

        // 1. 初始化进度到 Redis，并设置过期时间
        // 如果 Redis 不可用，立即返回错误，不创建任务
        boolean initSuccess = updateProgress(taskId,
                new TaskProgress("RUNNING", 0, "任务已提交，准备连接银行..."));
        if (!initSuccess) {
            System.out.println("Redis 错误:初始化失败，拒绝创建任务");
            LOGGER.error("Redis 初始化失败，拒绝创建任务，taskId=" + taskId);
            response.put("success", false);
            response.put("message", "redis数据库连接失败，请联系管理员");
            return response;
        }
        //Lambda 表达式, executorService.submit() 需要一个 Runnable 接口。
        //
        //Runnable 接口只有一个抽象方法 void run()，没有参数。
        //
        //Lambda 对应：() -> { ... }。
        //
        //()：表示 run 方法没有参数。
        //
        //{ ... }：里面包裹的就是 run 方法的具体实现。
        executorService.submit(() -> {
            try {
                Consumer<TaskProgress> progressCallback = (progress) -> updateProgress(taskId, progress);

                pdfCatchService.executeAsyncImport(startDate, endDate, company, Long.parseLong(formId),
                        progressCallback);
            } catch (Exception e) {
                LOGGER.error("异步任务执行异常", e);
                updateProgress(taskId, new TaskProgress("ERROR", 0, "系统异常: " + e.getMessage()));
            }
        });

        response.put("success", true);
        response.put("taskId", taskId);
        return response;
    }

    @GetMapping(path = "/dj/checkProgress.do", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TaskProgress checkProgress(@RequestParam("taskId") String taskId) {
        String key = REDIS_KEY_PREFIX + taskId;

        try {
            // 【修改】从 Redis 获取
            String json = stringRedisTemplate.opsForValue().get(key);

            if (json == null) {
                // Redis 里找不到了，说明过期了或者没创建
                return new TaskProgress("UNKNOWN", 0, "任务不存在或已过期");
            }

            // 反序列化回对象
            return gson.fromJson(json, TaskProgress.class);
        } catch (Exception e) {
            // Redis 连接失败或其他异常，返回降级信息
            LOGGER.error("Redis 获取进度失败，taskId=" + taskId, e);
            return new TaskProgress("ERROR", 0, "系统繁忙，无法获取任务进度，请稍后重试");
        }
    }

    private boolean updateProgress(String taskId, TaskProgress progress) {
        String key = REDIS_KEY_PREFIX + taskId;
        String json = gson.toJson(progress);

        try {
            // 【核心】写入 Redis 并重置过期时间
            // set(key, value, timeout, unit)
            stringRedisTemplate.opsForValue().set(key, json, EXPIRE_HOURS, TimeUnit.HOURS);
            return true; // 写入成功
        } catch (Exception e) {
            // Redis 连接失败时记录错误日志
            LOGGER.error("Redis 更新进度失败，taskId=" + taskId + ", progress=" + json, e);
            System.out.println("Redis 错误:更新进度失败，taskId=" + taskId + ", progress=" + json+",错误:"+e);
            return false; // 写入失败
        }
    }
}