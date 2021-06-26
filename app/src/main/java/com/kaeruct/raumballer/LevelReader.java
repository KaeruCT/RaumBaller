package com.kaeruct.raumballer;

import com.kaeruct.raumballer.wave.Wave;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;

public class LevelReader {

    private final BufferedReader reader;
    private final AndroidGame game;
    private String[] waveClasses;
    private String currentLine;
    private int wavesDone;
    private boolean complete;

    public LevelReader(AndroidGame game, InputStream inputStream) throws UnsupportedEncodingException {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        this.currentLine = null;
        this.wavesDone = 0;
        this.game = game;
        this.complete = false;
        init();
    }

    private void init() {
        this.executeNextLine();
    }

    private void executeNextLine() {
        try {
            this.currentLine = reader.readLine();
        } catch (IOException e) {
            this.game.dbgPrint(e.toString());
        }

        if (this.currentLine != null) {
            executeCurrentLine();
        } else {
            this.levelFinished();
        }
    }

    private void executeCurrentLine() {
        wavesDone = 0;
        String[] params = currentLine.split("\\+");
        waveClasses = new String[params.length];
        int[] maxAmounts = new int[params.length];

        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
            String[] cl = params[i].split("[ \t]");
            waveClasses[i] = cl[0].trim();
            maxAmounts[i] = Integer.parseInt(cl[1].trim());
        }

        if (waveClasses.length > 0) {
            createWaves(waveClasses, maxAmounts);
        }
    }

    private void createWaves(String[] classNames, int[] maxAmounts) {
        for (int i = 0; i < classNames.length; i++) {
            try {
                Class<Wave> wave = (Class<Wave>) Class.forName("com.kaeruct.raumballer.wave." + classNames[i]);
                Constructor<Wave> c = wave.getConstructor(AndroidGame.class, LevelReader.class, int.class);

                Wave w = c.newInstance(this.game, this, maxAmounts[i]);
                this.game.waves.add(w);

            } catch (Exception e) {
                this.game.dbgPrint(e.toString());
            }
        }
    }

    public void waveDone() {
        this.wavesDone++;

        if (this.wavesDone >= this.waveClasses.length) {
            this.executeNextLine();
        }
    }

    public void levelFinished() {
        try {
            complete = true;
            this.reader.close();
        } catch (IOException e) {
            this.game.dbgPrint(e.toString());
        }
    }

    public Boolean isComplete() {
        return complete;
    }
}
