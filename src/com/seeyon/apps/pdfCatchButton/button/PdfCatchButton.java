package com.seeyon.apps.pdfCatchButton.button;
import com.seeyon.cap4.form.bean.button.CommonBtn;
import com.seeyon.cap4.form.util.Enums;
import com.seeyon.ctp.common.log.CtpLogFactory;
import org.apache.commons.logging.Log;
import org.springframework.stereotype.Component;


@Component
public class PdfCatchButton extends CommonBtn {
    private static final Log LOOGGER = CtpLogFactory.getLog(PdfCatchButton.class);
    @Override
    public boolean canUse(Enums.FormType formType) {
        return true;
    }
    @Override
    public void init() {
        LOOGGER.info("正在初始化controller测试按钮");
        this.setPluginId("pdfCatchButton");//设置插件或者组件id，和pluginCfg.xml中的id一致
        this.setIcon("cap-icon-custom-button");

    }
    @Override
    public String getKey() {
        return "654321";//给按钮设置一个key，可以随便取，只是不要和已有按钮冲突;
    }

    @Override
    public String getNameSpace() {
        return "customBtn_" + this.getKey();
    }

    @Override
    public String getText() {
        return "全量数据更新";
    }

    @Override
    public String getPCInjectionInfo() {
        LOOGGER.info("正在注入pc666666666666666666666");
        return "{\"path\":\"apps_res/cap/customCtrlResources/def666/\",\"jsUri\":\"js/PdfCatchButton.js\",\"initMethod\":\"init\",\"nameSpace\":\"" + this.getNameSpace() + "\"}";
    }

    @Override
    public String getMBInjectionInfo() {
        return null;
    }


}