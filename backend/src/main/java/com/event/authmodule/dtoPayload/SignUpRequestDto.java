package com.event.authmodule.dtoPayload;

import com.event.authmodule.entitys.type.RoleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignUpRequestDto {

    private String username;
    private String password;

    Set<RoleType> roles = new HashSet<>();

}
