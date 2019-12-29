package com.example.androidgamecontrollers;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Joystick {

    private final int MIN_RADIUS = 50;
    private float centerPosX;
    private float centerPosY;
    private float joystickPosX;
    private float joystickPosY;

    private Paint joystickPaint;
    private Paint paddingPaint;

    private float joystickRadius;
    private float joystickBoundRadius;

    private float actuatorX;
    private float actuatorY;

    private boolean isPressed = false;

    public Joystick(float centerPosX, float centerPosY, float radius, float padding, int innerColour, int outerColor) {
        this.centerPosX = centerPosX;
        this.centerPosY = centerPosY;
        joystickPosX = centerPosX;
        joystickPosY = centerPosY;

        joystickRadius = radius > MIN_RADIUS? radius: MIN_RADIUS;
        joystickBoundRadius = padding > 0? radius + padding: radius * 3f; //joystickRadius + (joystickRadius * 1.5f) -> padding

        joystickPaint = new Paint();
        paddingPaint = new Paint();
        joystickPaint.setColor(innerColour);
        paddingPaint.setColor(outerColor);
    }

    public void draw(Canvas canvas) {
        canvas.drawCircle(centerPosX, centerPosY, joystickBoundRadius, paddingPaint);
        canvas.drawCircle(joystickPosX, joystickPosY, joystickRadius, joystickPaint);
    }

    public void update() {
        updateJoystickPosition();
    }

    private void updateJoystickPosition() {
        joystickPosX = centerPosX + actuatorX * joystickBoundRadius;
        joystickPosY = centerPosY + actuatorY * joystickBoundRadius;
    }

    public boolean isPressed(float x, float y) {
        return getTouchDistance(x, y) <= joystickBoundRadius;
    }

    public void setIsPressed(boolean isPressed) {
        this.isPressed = isPressed;
    }

    public boolean getIsPressed() {
        return isPressed;
    }

    public void setActuator(float x, float y) {
        float deltaX = x - centerPosX;
        float deltaY = y - centerPosY;

        float touchDistance = getTouchDistance(x, y);

        //Vector normalization
        if(touchDistance <= joystickBoundRadius) {
            actuatorX = deltaX/ joystickBoundRadius;
            actuatorY = deltaY/ joystickBoundRadius;
        } else {
            actuatorX = deltaX/touchDistance;
            actuatorY = deltaY/touchDistance;
        }
    }

    public void resetActuator() {
        actuatorX = 0;
        actuatorY = 0;
    }

    private float getTouchDistance(float x, float y) {
        return (float)Math.sqrt(Math.pow(centerPosX - x, 2) + Math.pow(centerPosY - y, 2));
    }

    public float getActuatorX() {
        return actuatorX;
    }

    public float getActuatorY() {
        return actuatorY;
    }
}
