package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.impl.FakeImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private FakeImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private Boolean isContainsCat = false;

    public SecurityService(SecurityRepository securityRepository, FakeImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    public SecurityService(SecurityService securityService) {
        this.securityRepository = securityService.securityRepository;
        this.imageService = securityService.imageService;
        // Copy status listeners if needed
        this.statusListeners.addAll(securityService.statusListeners);
        this.isContainsCat = securityService.isContainsCat;
    }


    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        if (armingStatus.equals(ArmingStatus.ARMED_HOME) && isContainsCat){
            setAlarmStatus(AlarmStatus.ALARM);
        }

        if (armingStatus.equals(ArmingStatus.ARMED_AWAY) || armingStatus.equals(ArmingStatus.ARMED_HOME)){
            List<Sensor> sensors = securityRepository.getSensors().stream().toList();
            sensors.forEach(sensor -> changeSensorActivationStatus(sensor, false));
        }
        securityRepository.setArmingStatus(armingStatus);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        isContainsCat = cat;

        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; // Không vấn đề nếu hệ thống đã vô hiệu hóa
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM:
                setAlarmStatus(AlarmStatus.PENDING_ALARM);
                break;
            case PENDING_ALARM:
                setAlarmStatus(AlarmStatus.ALARM);
                break;
            default:
                // Xử lý trường hợp mặc định nếu cần
                break;
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM:
                setAlarmStatus(AlarmStatus.NO_ALARM);
                break;
            case ALARM:
                setAlarmStatus(AlarmStatus.PENDING_ALARM);
                break;
            default:
                // Xử lý trường hợp mặc định nếu cần
                break;
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        AlarmStatus actualAlarmStatus = securityRepository.getAlarmStatus();
        if(actualAlarmStatus != AlarmStatus.ALARM){
            if(active) {
                handleSensorActivated();
            }
            if (sensor.getActive() && !active) {
                handleSensorDeactivated();
            }
        }

        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return Collections.unmodifiableSet(securityRepository.getSensors());
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

}

