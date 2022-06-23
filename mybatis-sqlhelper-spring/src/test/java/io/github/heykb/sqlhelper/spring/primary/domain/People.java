package io.github.heykb.sqlhelper.spring.primary.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
* 
* @author kb
*/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class People {
    private String name;
    private Integer age;
    private String email;
    private String deptId;
    private String id;
    private String createdBy;
    private String updatedBy;
    private String tenantId;
}
