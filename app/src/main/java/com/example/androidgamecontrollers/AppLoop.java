package com.example.androidgamecontrollers;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

class AppLoop extends Thread{
    private static final double MAX_UPS = 30.0;
    private static final double UPS_PERIOD = 1E+3/MAX_UPS;
    private JoystickManager joystickManager;
    private boolean isRunning = false;
    private SurfaceHolder surfaceHolder;
    private double averageUPS;
    private double averageFPS;

    public AppLoop(JoystickManager joystickManager, SurfaceHolder surfaceHolder) {
        this.joystickManager = joystickManager;
        this.surfaceHolder = surfaceHolder;
    }

    public double getAverageUPS() {
        return averageUPS;
    }

    public double getAverageFPS() {
        return averageFPS;
    }

    public void startLoop() {
        isRunning = true;
        start();
    }

    @Override
    public void run() {
        super.run();

        //Declare time and cycle count variables
        int updateCount = 0;
        int frameCount = 0;

        long startTime;
        long elapsedTime;
        long sleepTime;

        //Game loop
        Canvas canvas = null;
        startTime = System.currentTimeMillis();
        while (isRunning) {
            //Update and render game
            try {
                canvas = surfaceHolder.lockCanvas();

                synchronized (surfaceHolder) {
                    joystickManager.update();
                    updateCount++;

                    joystickManager.draw(canvas);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                        frameCount++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Pause game loop to not exceed target UPS
            elapsedTime = System.currentTimeMillis() - startTime;
            sleepTime = (long) (updateCount * UPS_PERIOD - elapsedTime);
            if(sleepTime > 0) {
                try {
                    sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //Skip frames to maintain target UPS

            //Calculate average UPS and FPS
            elapsedTime = System.currentTimeMillis() - startTime;
            if(elapsedTime >= 1000) {
                averageUPS = updateCount / (1E-3 * elapsedTime);
                averageFPS = frameCount / (1E-3 * elapsedTime);

                updateCount = 0;
                frameCount = 0;
                startTime = System.currentTimeMillis();
            }
        }
    }
}
