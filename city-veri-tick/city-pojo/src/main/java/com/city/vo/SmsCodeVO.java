package com.city.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class SmsCodeVO implements Serializable {
    private int cooldownSeconds;
}
