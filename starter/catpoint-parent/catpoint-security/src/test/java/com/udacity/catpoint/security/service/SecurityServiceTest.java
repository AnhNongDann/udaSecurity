package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.impl.FakeImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @InjectMocks
    private SecurityService securityService;

    @Mock
    private StatusListener statusListener;

    @Mock
    private FakeImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    private Sensor sensor;

    private Set<Sensor> getAllSensors(boolean status) {
        Random random = new Random();
        int number = Math.abs(random.nextInt()) % 10 + 1;
        Set<Sensor> sensors = new HashSet<>();
        SensorType[] sensorTypes = SensorType.values();

        for (int i = 0; i < number; i++) {
            sensors.add(new Sensor("testing sensor " + i, sensorTypes[Math.abs(random.nextInt()) % sensorTypes.length]));
        }
        sensors.forEach(sensor -> sensor.setActive(status));

        return sensors;
    }

    private BufferedImage getImageInstance() {
        return new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
    }

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        SensorType[] sensorTypes = SensorType.values();
        Random random = new Random();
        sensor = new Sensor("Testing", sensorTypes[Math.abs(random.nextInt()) % sensorTypes.length]);
    }

    // Test content 1:
    // If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @Test
    void changeSensorActivationStatus_AlarmIsArmedAndSensorIsActivated_SystemIsPendingAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test content 2:
    // If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @Test
    void changeSensorActivationStatus_AlarmIsArmedAndSensorIsActivatedAndSystemIsPendingAlarm_StatusIsAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test content 3:
    // If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    void changeSensorActivationStatus_PendingAlarmAndAllSensorsInactive_ReturnToNoAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test content 4:
    // If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    void changeSensorActivationStatus_AlarmIsActive_SensorStateChangeDoesNotAffectAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test content 5:
    // If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    void changeSensorActivationStatus_SensorActivatedWhileActiveAndSystemInPendingState_StatusIsAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test content 6:
    // If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void changeSensorActivationStatus_SensorDeactivatedWhileInactive_NoChangesToAlarmState(AlarmStatus status) {
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test content 7:
    // If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    void processImage_ImageContainsCatAndSystemArmedHome_StatusIsAlarm() {
        BufferedImage catImage = getImageInstance();
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(catImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test content 8:
    // If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    void processImage_ImageDoesNotContainCat_StatusIsNoAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test content 9:
    // If the system is disarmed, set the status to no alarm.
    @Test
    void setArmingStatus_SystemDisarmed_StatusIsNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test content 10:
    // If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void setArmingStatus_SystemArmed_ResetSensorsToInactive(ArmingStatus status) {
        Set<Sensor> sensors = getAllSensors(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);
        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }

    // Test content 11:
    // If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    void setArmingStatus_SystemArmedHomeAndImageContainsCat_StatusIsAlarm() {
        BufferedImage catImage = getImageInstance();
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

}
