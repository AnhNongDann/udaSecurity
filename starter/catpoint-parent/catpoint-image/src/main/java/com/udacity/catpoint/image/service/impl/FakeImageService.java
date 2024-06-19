package com.udacity.catpoint.image.service.impl;

import com.udacity.catpoint.image.service.BaseImageService;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Service that tries to guess if an image displays a cat.
 */
public class FakeImageService implements BaseImageService {
    private final Random r = new Random();

    public boolean imageContainsCat(BufferedImage image, float confidenceThreshhold) {
        return r.nextBoolean();
    }
}
