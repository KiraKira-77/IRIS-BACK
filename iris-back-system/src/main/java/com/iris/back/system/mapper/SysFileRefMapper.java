package com.iris.back.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iris.back.system.model.dto.FileRefView;
import com.iris.back.system.model.entity.SysFileRefEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysFileRefMapper extends BaseMapper<SysFileRefEntity> {

  @Select("""
      <script>
      SELECT
        r.biz_id AS bizId,
        f.id AS fileId,
        f.file_name AS fileName,
        f.content_type AS contentType,
        f.file_size AS fileSize,
        f.object_key AS objectKey,
        f.bucket_name AS bucketName,
        COALESCE(u.username, CAST(f.created_by AS CHAR)) AS uploadedBy,
        f.created_at AS uploadedAt
      FROM sys_file_ref r
      INNER JOIN sys_file f ON f.id = r.file_id AND f.deleted = 0
      LEFT JOIN sys_user u ON u.id = f.created_by AND u.deleted = 0
      WHERE r.tenant_id = #{tenantId}
        AND r.biz_type = #{bizType}
        AND r.deleted = 0
        AND r.biz_id IN
        <foreach collection="bizIds" item="bizId" open="(" separator="," close=")">
          #{bizId}
        </foreach>
      ORDER BY r.biz_id, r.sort_no, r.id
      </script>
      """)
  List<FileRefView> selectByBizIds(
      @Param("tenantId") Long tenantId,
      @Param("bizType") String bizType,
      @Param("bizIds") List<Long> bizIds
  );
}
