package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import com.kotcrab.vis.ui.widget.file.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import de.felixp.fractalsgdx.animation.AnimationListener;
import de.felixp.fractalsgdx.animation.ParamAnimation;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixperko.fractals.util.NumberUtil;

public class ScreenshotRecordUI {

    static String commandString = null;
    static VideoEncoding selectedVideoEncoding = VideoEncoding.libx265;

    static void openRecordAnimationWindow(MainStage stage) {
        //TODO
        VisWindow recordAnimationWindow = new VisWindow("Record animation frames");
        recordAnimationWindow.addCloseButton();

        String currentPathSetting = ScreenshotUI.screenshotFolderPath;
        String currentFolderSetting = ScreenshotUI.getScreenshotFileName();
        String currentFfmpegFolderSetting = "D:\\Downloads\\ffmpeg-4.3.1-2021-01-01-full_build\\bin";
        VisTextField pathField = new VisTextField(currentPathSetting){
            @Override
            public float getPrefWidth() {
                return 500;
            }
        };
        VisTextField folderField = new VisTextField(currentFolderSetting){
            @Override
            public float getPrefWidth() {
                return 500;
            }
        };
        VisTextField ffmpegFolderField = new VisTextField(currentFfmpegFolderSetting){
            @Override
            public float getPrefWidth() {
                return 500;
            }
        };

        FileChooser.setDefaultPrefsName("de.felixp.fractalsgdx.ui.filechooser");
        FileChooser fileChooser = new FileChooser(currentPathSetting, FileChooser.Mode.OPEN);
        Class<FileChooser> fileChooserClass = FileChooser.class;
        for (Field field : fileChooserClass.getDeclaredFields()){
            if (field.getName().equals("confirmButton")){
                field.setAccessible(true);
                try {
                    VisTextButton confirmButton = (VisTextButton) field.get(fileChooser);
                    confirmButton.setText("Choose Folder");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                field.setAccessible(false);
                break;
            }
        }
        fileChooser.setSelectionMode(FileChooser.SelectionMode.DIRECTORIES);
        pathField.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                pathField.focusLost();
                stage.addActor(fileChooser.fadeIn());
                fileChooser.setSize(Gdx.graphics.getWidth()*0.7f, Gdx.graphics.getHeight()*0.7f);
                fileChooser.centerWindow();
            }
        });

        VisSelectBox<ParamAnimation> animationsSelect = new VisSelectBox();
        FractalRenderer renderer = stage.getFocusedRenderer();
        List<ParamAnimation> animationList = new ArrayList<>();
        if (renderer != null){
            animationList.addAll(renderer.getRendererContext().getParameterAnimations());
        }
        animationsSelect.setItems(animationList.toArray(new ParamAnimation[animationList.size()]));
        VisTextField framerateField = new VisValidatableTextField(Validators.INTEGERS);
        framerateField.setText("60");
        VisTextField qualityField = new VisValidatableTextField(Validators.INTEGERS);
        int quality = selectedVideoEncoding == VideoEncoding.libx265 ? 28 : 23;
        qualityField.setText(quality+"");

        VisTextArea commandArea = new VisTextArea("Command goes here..."){
            @Override
            public float getPrefWidth() {
                return 500;
            }

            @Override
            public float getPrefHeight() {
                return 200;
            }
        };

        refreshCommand(pathField, folderField, ffmpegFolderField, commandArea, framerateField, qualityField, animationsSelect);
        pathField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                refreshCommand(pathField, folderField, ffmpegFolderField, commandArea, framerateField, qualityField, animationsSelect);
            }
        });
        folderField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                refreshCommand(pathField, folderField, ffmpegFolderField, commandArea, framerateField, qualityField, animationsSelect);
            }
        });
        ffmpegFolderField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ffmpegFolderField.setInputValid(checkFFmpegFolder(ffmpegFolderField.getText()));
                refreshCommand(pathField, folderField, ffmpegFolderField, commandArea, framerateField, qualityField, animationsSelect);
            }
        });
        framerateField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                refreshCommand(pathField, folderField, ffmpegFolderField, commandArea, framerateField, qualityField, animationsSelect);
            }
        });
        qualityField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                refreshCommand(pathField, folderField, ffmpegFolderField, commandArea, framerateField, qualityField, animationsSelect);
            }
        });

        fileChooser.setListener(new FileChooserAdapter() {
            @Override
            public void selected (Array<FileHandle> files) {
                pathField.setText(files.first().file().getAbsolutePath());
                refreshCommand(pathField, folderField, ffmpegFolderField, commandArea, framerateField, qualityField, animationsSelect);
            }
        });

        VisCheckBox saveAnimationCheckbox = new VisCheckBox("encode video");
        VisCheckBox deleteScreenshotsCheckbox = new VisCheckBox("delete images");

        VisTextButton cancelButton = new VisTextButton("Cancel", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                recordAnimationWindow.remove();
            }
        });
        VisTextButton renderButton = new VisTextButton("Render", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ParamAnimation animation = animationsSelect.getSelected();
                String path = getOutputPath(pathField, folderField);
                openRecordAnimationProgressWindow(stage, animation, path, commandString, deleteScreenshotsCheckbox.isChecked());
                ScreenshotUI.recordScreenshots(animation, renderer, path.substring(0, path.length()-1));
                recordAnimationWindow.remove();
            }
        });
        if (animationsSelect.getItems().isEmpty())
            renderButton.setDisabled(true);

        VisTable animationsTable = new VisTable(true);

        saveAnimationCheckbox.setChecked(true);
        deleteScreenshotsCheckbox.setChecked(true);
        saveAnimationCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean savingAnimation = saveAnimationCheckbox.isChecked();
                deleteScreenshotsCheckbox.setDisabled(!savingAnimation);
                if (savingAnimation) {
                    initAnimationsTable(ffmpegFolderField, framerateField, qualityField, commandArea, animationsTable);
                    recordAnimationWindow.pack();
                } else {
                    animationsTable.clear();
                    deleteScreenshotsCheckbox.setChecked(false);
//                    recordAnimationWindow.pack();
                }
            }
        });

        VisTable contentTable = new VisTable(true);

        contentTable.add("Output path: ").left();
        contentTable.add(pathField).fillX().expandX().row();

        contentTable.add("Folder name: ").left();
        contentTable.add(folderField).fillX().expandX().row();

        contentTable.add("Animation: ").left();
        contentTable.add(animationsSelect).left().row();

        contentTable.add("Options: ");
        VisTable optionsTable = new VisTable(true);
        optionsTable.add(saveAnimationCheckbox).left();
        optionsTable.add(deleteScreenshotsCheckbox).left();
        contentTable.add(optionsTable).left().row();

        initAnimationsTable(ffmpegFolderField, framerateField, qualityField, commandArea, animationsTable);

        contentTable.add(animationsTable).fillX().fillY().expandX().expandY().colspan(2).row();

        VisTable buttonTable = new VisTable(true);
        buttonTable.add(cancelButton);
        buttonTable.add(renderButton);
        contentTable.add(buttonTable).colspan(2).row();

        recordAnimationWindow.add(contentTable).fillX().fillY().expandX().expandY();

        stage.addActor(recordAnimationWindow);
        recordAnimationWindow.pack();
        recordAnimationWindow.centerWindow();
        recordAnimationWindow.setResizable(true);

    }

    private static void openRecordAnimationProgressWindow(MainStage stage, ParamAnimation animation, String path, String command, boolean deleteImages) {
        VisWindow progressWindow = new VisWindow("Recording animation frames");
        progressWindow.addCloseButton();

        VisTextButton cancelButton = new VisTextButton("cancel", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                animation.setPaused(true);
                //TODO remove screenshot flag, delete files
                progressWindow.remove();
            }
        });

        VisTextButton pauseButton = new VisTextButton("pause");
        pauseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean setPaused = !animation.isPaused();
                animation.setPaused(setPaused);
                pauseButton.setText(setPaused ? "resume" : "pause");
                progressWindow.pack();
            }
        });

        VisTextButton hideButton = new VisTextButton("hide", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                progressWindow.remove();
            }
        });


        VisTextButton openExplorerButton = new VisTextButton("open folder", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                try {
                    FileUtils.showDirInExplorer(new FileHandle(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        VisTable contentTable = new VisTable(true);


        VisLabel stageLabel = new VisLabel("Saving images 0/"+animation.getFrameCount()+" (0 mb)");
        VisLabel progressLabel = new VisLabel("0.0%");

        contentTable.add("Stage: ").left();
        contentTable.add(stageLabel).left().row();
        contentTable.add("Progress: ").left();
        contentTable.add(progressLabel).left().row();

        VisTable buttonTable = new VisTable(true);
        buttonTable.add(cancelButton);
        buttonTable.add(pauseButton);
        buttonTable.add(hideButton);
        buttonTable.add(openExplorerButton);
        contentTable.add(buttonTable).colspan(2);

        progressWindow.add(contentTable);

        stage.addActor(progressWindow);
        progressWindow.pack();
        progressWindow.centerWindow();
        progressWindow.setResizable(true);

        Timer.Task updateRunnable = getUpdateRunnable(stageLabel, progressLabel, animation);
        Timer.schedule(updateRunnable, 0, 0.2f);
        animation.addAnimationListener(new AnimationListener() {
            @Override
            public void animationFinished() {
                animation.removeAnimationListener(this);
                updateRunnable.cancel();
                try {
                    Process process = Runtime.getRuntime().exec(command);
//                    BufferedReader processOut = new BufferedReader(new
//                            InputStreamReader(process.getInputStream()));
//                    BufferedReader processErr = new BufferedReader(new
//                            InputStreamReader(process.getErrorStream()));

                    Timer.schedule(getFFmpegUpdateRunnable(stageLabel, progressLabel, animation, process, path, deleteImages), 0, 0.2f);

                } catch (IOException e) {
                    stageLabel.setText("Error: "+e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void animationProgressUpdated() {

            }
        });
    }

    private static Timer.Task getUpdateRunnable(VisLabel stageLabel, VisLabel progressLabel, ParamAnimation animation){
        return new Timer.Task() {
            @Override
            public void run() {
                int frameCounter = animation.getFrameCounter();
                int frameCount = animation.getFrameCount();
                stageLabel.setText("Saving images "+ frameCounter +"/"+ frameCount);
                double progress = frameCounter * 50.0 / frameCount;
                progressLabel.setText(NumberUtil.getRoundedDouble(progress, 1)+"%");
            }
        };
    }

    static int encodeFrame = -1;

    private static Timer.Task getFFmpegUpdateRunnable(VisLabel stageLabel, VisLabel progressLabel, ParamAnimation animation, Process process, String path, boolean deleteImages){
        encodeFrame = -1;
        return new Timer.Task() {
            @Override
            public void run() {
                boolean done = false;

                //process log output of FFmpeg

                File logfile = new File(path + "log.txt");
                if (logfile.exists() && logfile.length() > 0) {

                    BufferedReader in = null;
                    try {
                        in = new BufferedReader(new InputStreamReader(new FastReverseLineInputStream(logfile)));

                        while (true) {

                            String line = in.readLine();
                            if (line == null) {
                                break;
                            }

                            if (line.contains("progress=end"))
                                done = true;
                            else if (line.contains("frame=")) {
                                String frameStr = line.split("=")[1];
                                encodeFrame = Integer.parseInt(frameStr);
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

//                String line = null;
//                try {
//                    if ((line = processOut.readLine()) != null) {
//                        if (line.startsWith("frame=")){
//                            try {
//                                encodeFrame = Integer.parseInt(line.split("=")[1]);
//                            } catch (Exception e){
//                                e.printStackTrace();
//                            }
//                        }
//                        else if (line.startsWith("progress=end")){
//                            done = true;
//                            cancel();
//                            finishRecoding(stageLabel, progressLabel, animation, path, deleteImages);
//                        }
//                        System.out.println("FFmpeg process OUT: "+line);
//                    }
//                    if ((line = processErr.readLine()) != null) {
//                        System.err.println("FFmpeg process ERR: "+line);
//                    }
//                } catch (IOException e){
//                    e.printStackTrace();
//                }
//                done = !process.isAlive();
                if (!done) {
                    int frameCount = animation.getFrameCount();
                    stageLabel.setText(encodeFrame == -1 ? "Encoding video" : "Encoding video " + encodeFrame + "/" + frameCount);
                    double progress = encodeFrame == -1 ? 50.0 : 50.0 + encodeFrame * 50.0 / frameCount;
                    progressLabel.setText(NumberUtil.getRoundedDouble(progress, 1) + "%");
                }
                else {
                    finishRecoding(stageLabel, progressLabel, animation, path, deleteImages);
                }
            }
        };
    }

    private static void finishRecoding(VisLabel stageLabel, VisLabel progressLabel, ParamAnimation animation, String path, boolean deleteImages) {
        if (deleteImages){
            stageLabel.setText("deleting images...");
            progressLabel.setText("99.9%");
            File folder = new File(path);
            for (File f : folder.listFiles()){
                if (f.getName().endsWith(".png"))
                    f.delete();
            }
        }
        stageLabel.setText("finished");
        progressLabel.setText("100.0%");
    }

    private static void initAnimationsTable(VisTextField ffmpegFolderField, VisTextField framerateField, VisTextField qualityField, VisTextArea commandArea, VisTable animationsTable) {
        animationsTable.add("FFmpeg path: ").left();
        animationsTable.add(ffmpegFolderField).fillX().expandX().row();

        animationsTable.add("Framerate: ").left();
        animationsTable.add(framerateField).left().row();

        animationsTable.add("Quality: ").left();
        animationsTable.add(qualityField).left().row();

        animationsTable.add("Command: ").left();
        animationsTable.add(commandArea).fillX().fillY().expandX().expandY();
    }

    private static void refreshCommand(VisTextField pathField, VisTextField folderField, VisTextField ffmpegFolderField, VisTextArea commandArea, VisTextField framerateField, VisTextField qualityField, VisSelectBox<ParamAnimation> animationsSelect) {
        int frameRate = Integer.parseInt(framerateField.getText());
        int frameCount = animationsSelect.getSelected().getAnimationFrameCount(frameRate);
        int digits = (int)Math.ceil(Math.log10(frameCount));
        String screenshotFolder = getOutputPath(pathField, folderField);
        String ffmpegFolder = ffmpegFolderField.getText();
        String ffmpegPath = ffmpegFolder+"\\ffmpeg.exe";
        if (!framerateField.isInputValid() || !qualityField.isInputValid())
            return;
        int crf = Integer.parseInt(qualityField.getText());

        boolean ffmpegDetected = checkFFmpegFolder(ffmpegFolder);
        ffmpegFolderField.setInputValid(ffmpegDetected);

        String imgExt = ScreenshotUI.extensionSelect.getSelected();

//        commandArea.setText("D:\\Downloads\\ffmpeg-4.3.1-2021-01-01-full_build\\bin\\ffmpeg.exe -framerate 60 -i %03d.png -crf 25 output.mp4");
        if (selectedVideoEncoding == VideoEncoding.libx265)
            commandString = ffmpegPath+" -framerate "+frameRate+" -i "+screenshotFolder+"%0"+digits+"d"+imgExt+ " " +
                    "-c:v libx265 -preset medium " +
                    "-crf "+crf+" -progress "+screenshotFolder+"log.txt -nostats "+screenshotFolder+"output.mp4";
        else
            commandString = ffmpegPath+" -framerate "+frameRate+" -i "+screenshotFolder+"%0"+digits+"d"+imgExt+ " " +
                    "-crf "+crf+" -progress "+screenshotFolder+"log.txt -nostats "+screenshotFolder+"output.mp4";
        commandArea.setText(commandString);
    }

    private static boolean checkFFmpegFolder(String ffmpegFolder) {
        String ffmpegPath = ffmpegFolder;
        try {
            if (!ffmpegFolder.startsWith("ffmpeg")) {
                ffmpegPath += "\\ffmpeg.exe";
                File file = new File(ffmpegPath);
                if (!file.exists())
                    return false;
            }
//            else {
//                ffmpegPath = System.getenv()
//            }
            Process process = Runtime.getRuntime().exec(ffmpegPath+" -version");

//            ProcessBuilder builder = new ProcessBuilder(ffmpegPath+" -version");
//            builder.inheritIO();
//            builder.start();

//            BufferedReader processOut = new BufferedReader(new
//                    InputStreamReader(process.getInputStream()));
//            BufferedReader processErr = new BufferedReader(new
//                    InputStreamReader(process.getErrorStream()));


//            System.out.println("------------------------------------");
//            System.out.println("ffmpeg version check out/err:");
//            System.out.println("------------------------------------");
//            String out = null;
//            while ((out = processOut.readLine()) != null){
//                System.out.println(out);
//            }
//            System.out.println("------------------------------------");
//            while ((out = processErr.readLine()) != null){
//                System.out.println(out);
//            }
//            System.out.println("------------------------------------");

            return true;
        } catch (IOException e) {
//            e.printStackTrace();
            return false;
        }
    }

    private static String getOutputPath(VisTextField pathField, VisTextField folderField) {
        return pathField.getText()+"\\"+folderField.getText()+"\\";
    }

    enum VideoEncoding{
        libx264, libx265, other;
    }
}

/*
 *  Source: https://stackoverflow.com/a/34588482
 */
class FastReverseLineInputStream extends InputStream {

    private static final int MAX_LINE_BYTES = 1024 * 1024;

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;

    private RandomAccessFile in;

    private long currentFilePos;

    private int bufferSize;
    private byte[] buffer;
    private int currentBufferPos;

    private int maxLineBytes;
    private byte[] currentLine;
    private int currentLineWritePos = 0;
    private int currentLineReadPos = 0;
    private boolean lineBuffered = false;

    public FastReverseLineInputStream(File file) throws IOException {
        this(file, DEFAULT_BUFFER_SIZE, MAX_LINE_BYTES);
    }

    public FastReverseLineInputStream(File file, int bufferSize, int maxLineBytes) throws IOException {
        this.maxLineBytes = maxLineBytes;
        in = new RandomAccessFile(file, "r");
        currentFilePos = file.length() - 1;
        in.seek(currentFilePos);
        if (in.readByte() == 0xA) {
            currentFilePos--;
        }
        currentLine = new byte[maxLineBytes];
        currentLine[0] = 0xA;

        this.bufferSize = bufferSize;
        buffer = new byte[bufferSize];
        fillBuffer();
        fillLineBuffer();
    }

    @Override
    public int read() throws IOException {
        if (currentFilePos <= 0 && currentBufferPos < 0 && currentLineReadPos < 0) {
            return -1;
        }

        if (!lineBuffered) {
            fillLineBuffer();
        }


        if (lineBuffered) {
            if (currentLineReadPos == 0) {
                lineBuffered = false;
            }
            return currentLine[currentLineReadPos--];
        }
        return 0;
    }

    private void fillBuffer() throws IOException {
        if (currentFilePos < 0) {
            return;
        }

        if (currentFilePos < bufferSize) {
            in.seek(0);
            in.read(buffer);
            currentBufferPos = (int) currentFilePos;
            currentFilePos = -1;
        } else {
            in.seek(currentFilePos);
            in.read(buffer);
            currentBufferPos = bufferSize - 1;
            currentFilePos = currentFilePos - bufferSize;
        }
    }

    private void fillLineBuffer() throws IOException {
        currentLineWritePos = 1;
        while (true) {

            // we've read all the buffer - need to fill it again
            if (currentBufferPos < 0) {
                fillBuffer();

                // nothing was buffered - we reached the beginning of a file
                if (currentBufferPos < 0) {
                    currentLineReadPos = currentLineWritePos - 1;
                    lineBuffered = true;
                    return;
                }
            }

            byte b = buffer[currentBufferPos--];

            // \n is found - line fully buffered
            if (b == 0xA) {
                currentLineReadPos = currentLineWritePos - 1;
                lineBuffered = true;
                break;

                // just ignore \r for now
            } else if (b == 0xD) {
                continue;
            } else {
                if (currentLineWritePos == maxLineBytes) {
                    throw new IOException("file has a line exceeding " + maxLineBytes
                            + " bytes; use constructor to pickup bigger line buffer");
                }

                // write the current line bytes in reverse order - reading from
                // the end will produce the correct line
                currentLine[currentLineWritePos++] = b;
            }
        }
    }
}