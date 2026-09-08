package com.example.nawibackend.common.models.embadable;

import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class ZeroTareDeviceConfig {
    private boolean nonAutomaticZeroSetting;
    private boolean semiAutomaticZeroSetting;
    private boolean automaticZeroSetting;
    private boolean initialZeroSetting;
    private boolean zeroTracking;
    private boolean tareBalancing;
    private boolean tareWeighing;
    private boolean combinedZeroTareDevice;
    private boolean presetTareDevice;
    private boolean subtractiveTare;
    private boolean additiveTare;
}
