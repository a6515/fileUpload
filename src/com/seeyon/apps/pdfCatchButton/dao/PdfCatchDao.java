package com.seeyon.apps.pdfCatchButton.dao;

import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface PdfCatchDao {

    @Select("SELECT now()")
    Date showTime();

    List<String> selectExistingIds(@Param("tableName") String tableName,
                                   @Param("uniqueField") String uniqueField,
                                   @Param("list") List<String> list);


    /**
     * 根据业务参考号（Yurref）查询主表 ID
     * @param tableName 主表表名
     * @param yurrefValue 业务参考号的值
     * @return 主表 ID
     */
    Long selectMainIdByYurref(@Param("tableName") String tableName,
                              @Param("yurrefValue") String yurrefValue);

    /**
     * 根据主表ID和匹配条件（如姓名），查询子表 ID
     * @param subTableName 子表表名
     * @param mainId 主表 ID
     * @param matchValue 用于匹配的值 (例如 "张三")
     * @return 子表 ID
     */
    @Select("SELECT ID FROM ${subTableName} WHERE formmain_id = #{mainId} AND field0041 = #{matchValue}")
    Long selectSubId(@Param("subTableName") String subTableName,
                     @Param("mainId") Long mainId,
                     @Param("matchValue") String matchValue);

    /**
     * 根据主表ID和匹配条件（如姓名），更新子表的某个字段
     * @param subTableName 子表表名
     * @param updateValue 更新后的值
     * @param mainId 主表 ID
     * @param matchValue 用于匹配的值 (例如 "张三")
     */
    int updateSubTableValue(@Param("subTableName") String subTableName,
                            @Param("updateValue") Object updateValue,
                            @Param("mainId") Long mainId,
                            @Param("matchValue") String matchValue);



    @Select({
            "SELECT ID, FILENAME, MIME_TYPE, CREATE_DATE, FILE_SIZE",
            "FROM ctp_file",
            "WHERE ID = #{fileId}"
    })
    Map<String, Object> selectCtpFile(@Param("fileId") Long var1);

    @Insert({
            "INSERT INTO ctp_attachment (",
            "  ID, ATT_REFERENCE, SUB_REFERENCE, CATEGORY, TYPE, FILENAME,",
            "  FILE_URL, MIME_TYPE, CREATEDATE, ATTACHMENT_SIZE, SORT",
            ") VALUES (",
            "  #{id}, #{attReference}, #{subReference}, #{category}, #{type}, #{filename},",
            "  #{fileUrl}, #{mimeType}, #{createDate}, #{attachmentSize}, #{sort}",
            ")"
    })
    void insertAttachment(
            @Param("id") Long var1,
            @Param("attReference") Long var2,
            @Param("subReference") Long var3,
            @Param("category") Integer var4,
            @Param("type") Integer var5,
            @Param("filename") String var6,
            @Param("fileUrl") Long var7,
            @Param("mimeType") String var8,
            @Param("createDate") Date var9,
            @Param("attachmentSize") Long var10,
            @Param("sort") Integer var11
    );

    @Update({
            "UPDATE ${tableName}",
            "SET ${fieldName} = #{subReference}",
            "WHERE ID = #{masterId}"
    })
    void updateFormMain(
            @Param("tableName") String var1,
            @Param("fieldName") String var2,
            @Param("subReference") Long var3,
            @Param("masterId") Long var4
    );


}