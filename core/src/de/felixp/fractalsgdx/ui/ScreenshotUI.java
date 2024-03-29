package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisRadioButton;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.animation.AnimationListener;
import de.felixp.fractalsgdx.animation.ParamAnimation;
import de.felixp.fractalsgdx.rendering.renderers.FractalRenderer;
import de.felixp.fractalsgdx.rendering.ScreenshotListener;
import de.felixp.fractalsgdx.ui.actors.FractalsWindow;
import de.felixp.fractalsgdx.util.FractalsIOUtil;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.util.io.IIOMetadataUpdater;

public class ScreenshotUI {

    final static String EXTENSION_PNG = ".png";
    final static String EXTENSION_JPG = ".jpg";
    public static final String IMAGE_SELECT_OUTPUT = "Output preview";
    public static final String IMAGE_SELECT_ORIGINAL = "Original image";
    public static final String METADATA_KEY = "params";

    static FractalsWindow window;

    static VisTable folderTable;
    static VisTable advancedTable;
    static VisTable previewTable;
    static VisTable dynamicTable;
    static VisTable buttonTable;

    static VisLabel folderLabel;
    static VisTextField folderTextField;
    static VisLabel filenameLabel;
    static VisTextField filenameTextField;
    static VisSelectBox<String> extensionSelect;

    static VisLabel qualityLabel;
    static VisSlider qualitySlider;
    static VisLabel qualityValueLabel;

    static FileChooser fileChooser;

    static VisLabel sizeLabel;
//    static VisSelectBox<String> imageSelect;
    static VisRadioButton outputButton;
    static VisRadioButton originalButton;
    static ImageRenderer imageRenderer;

    static VisTextButton cancelButton;
    static VisTextButton recordAnimationButton;
    static VisTextButton previewButton;
    static VisTextButton saveButton;

    static Pixmap originalScreenshotPixmap;
    static Pixmap compressedScreenshotPixmap;

    static String screenshotFolderPath = null;

    static boolean previewVisible = false;

    public static void openScreenshotWindow(MainStage stage){

        previewVisible = false;

        String startExtensionSelectValue = EXTENSION_PNG;
        float startQualityValue = 95f;

        String screenshotFileName = getScreenshotFileName();

        if (window == null) {
            window = new FractalsWindow("Screenshot");
            ((VisWindow) window).addCloseButton();

            folderTable = new VisTable(true);
            advancedTable = new VisTable(true);
            previewTable = new VisTable(true);
            dynamicTable = new VisTable(true);

            folderLabel = new VisLabel("Folder: ");
//            folderTextField = new VisTextField(Gdx.files.external(screenshotFileName).parent().path()){
            if (screenshotFolderPath == null) {
                String userHome = System.getProperty("user.home");
                screenshotFolderPath = userHome;
            }
            folderTextField = new VisTextField(screenshotFolderPath){
                @Override
                public float getPrefWidth() {
                    return 500;
                }
            };

            filenameLabel = new VisLabel("File name: ");
            filenameTextField = new VisTextField(){
                @Override
                public float getPrefWidth() {
                    return 500;
                }
            };
            filenameTextField.setAlignment(Align.right);
            extensionSelect = new VisSelectBox<String>();

            FileChooser.setDefaultPrefsName("de.felixp.fractalsgdx.ui.filechooser");
            fileChooser = new FileChooser(screenshotFolderPath, FileChooser.Mode.OPEN);
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
            fileChooser.setListener(new FileChooserAdapter() {
                @Override
                public void selected (Array<FileHandle> files) {
                    screenshotFolderPath = files.first().file().getAbsolutePath();
                    folderTextField.setText(screenshotFolderPath);
                }
            });

            folderTextField.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    folderTextField.focusLost();
                    stage.addActor(fileChooser.fadeIn());
                    fileChooser.setSize(Gdx.graphics.getWidth()*0.7f, Gdx.graphics.getHeight()*0.7f);
                    fileChooser.centerWindow();
                }
            });
            extensionSelect.setItems(EXTENSION_PNG, EXTENSION_JPG);


            qualityLabel = new VisLabel("jpeg quality: ");
            qualitySlider = new VisSlider(0f, 100f, 1f, false);
            qualitySlider.setValue(startQualityValue);
            qualityValueLabel = new VisLabel((int)startQualityValue+"%");

            qualitySlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    qualityValueLabel.setText((int)qualitySlider.getValue()+"%");
                }
            });

            updateJpgQualityVisibility();
            extensionSelect.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
//                    if (EXTENSION_PNG.equals(extensionSelect.getSelected()))
                    previewVisible = false;
                    updateJpgQualityVisibility();
                    repopulateDynamicTable();
                    window.pack();
                    window.centerWindow();
                }
            });

            sizeLabel = new VisLabel("Size: ");
            outputButton = new VisRadioButton("output");
            outputButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    updatePreviewImage();
                }
            });
            originalButton = new VisRadioButton("original");
            originalButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    updatePreviewImage();
                }
            });
            ButtonGroup group = new ButtonGroup(outputButton, originalButton);
//            imageSelect = new VisSelectBox<>();
//            imageSelect.setItems(IMAGE_SELECT_OUTPUT, IMAGE_SELECT_ORIGINAL);
//            imageSelect.addListener(new ChangeListener() {
//                @Override
//                public void changed(ChangeEvent event, Actor actor) {
//                    updatePreviewImage();
//                }
//            });
            imageRenderer = new ImageRenderer();

            sizeLabel.setVisible(false);
//            imageSelect.setVisible(false);
            outputButton.setVisible(false);
            originalButton.setVisible(false);

            buttonTable = new VisTable(true);
            cancelButton = new VisTextButton("Cancel", new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    window.remove();
                }
            });
            recordAnimationButton = new VisTextButton("Animation...", new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    window.setAutoRefocus(false);
                    ScreenshotRecordUI.openRecordAnimationWindow(stage);
                    window.remove();
                }
            });
            previewButton = new VisTextButton("Preview", new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    previewScreenshot();
                }
            });
            saveButton = new VisTextButton("Save", new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    saveScreenshot();
                    sizeLabel.setVisible(false);
                    outputButton.setVisible(false);
                    originalButton.setVisible(false);
//                    imageSelect.setVisible(false);
//                    window.remove();
                }
            });


            folderTable.add(folderLabel).left();
            folderTable.add(folderTextField).fillX().colspan(2);
            folderTable.row();

            folderTable.add(filenameLabel).left();
            folderTable.add(filenameTextField);
            folderTable.add(extensionSelect).row();

            advancedTable.add("post-process mode:");
            VisSelectBox<String> modeSelect = new VisSelectBox<>();
            modeSelect.setItems("default", "raw data texture (planned)", "raw data file (planned)", "sample counts texture (planned)");
            //"raw escaped", "raw unescaped"
            advancedTable.add(modeSelect);

            buttonTable.add(cancelButton);
            buttonTable.add(recordAnimationButton);
            buttonTable.add(previewButton);
            buttonTable.add(saveButton);
            VisTable buttonWrapperTable = new VisTable(true);
            buttonWrapperTable.add(buttonTable);


            VisTable windowTable = new VisTable(true);
            windowTable.add(folderTable).row();
            windowTable.add(advancedTable).left().row();
            windowTable.add(dynamicTable).expandX().fillX().row();
            windowTable.add(buttonWrapperTable).row();

            window.add(windowTable);

//            window.debug();
        }
        else { //exists already
            sizeLabel.setVisible(false);
            outputButton.setVisible(false);
            originalButton.setVisible(false);
//                    imageSelect.setVisible(false);
        }

        repopulateDynamicTable();

        imageRenderer.setDimensions(0, 0);
        imageRenderer.invalidate();

        outputButton.setChecked(true);

        filenameTextField.setText(getScreenshotFileName());

        stage.addActor(window);
        window.pack();
        window.centerWindow();
    }

    private static void repopulateDynamicTable() {

        dynamicTable.clear();

        boolean jpg = EXTENSION_JPG.equals(extensionSelect.getSelected());
        if (jpg) {
            dynamicTable.add(qualityLabel);
            dynamicTable.add(qualitySlider).expandX().fillX();
            dynamicTable.add(qualityValueLabel).row();

            dynamicTable.add(outputButton).left();
            dynamicTable.add(originalButton).left();
            dynamicTable.add(sizeLabel).left().row();
        }
        if (previewVisible)
            dynamicTable.add(imageRenderer).colspan(3).center();
    }

    private static void updatePreviewImage() {
        if (imageRenderer == null)
            return;

        if (outputButton.isChecked() && compressedScreenshotPixmap != null) {
            imageRenderer.setPixmap(compressedScreenshotPixmap);
        }
        else if (originalButton.isChecked()) {
            imageRenderer.setPixmap(originalScreenshotPixmap);
        }
//        String value = imageSelect.getSelected();
//        if (value.equalsIgnoreCase(IMAGE_SELECT_ORIGINAL))
//            imageRenderer.setPixmap(originalScreenshotPixmap);
//        else if (value.equalsIgnoreCase(IMAGE_SELECT_OUTPUT))
//            imageRenderer.setPixmap(compressedScreenshotPixmap);
    }

    private static void updateJpgQualityVisibility() {
        String currExt = extensionSelect.getSelected();
        boolean showJpgQuality = currExt.equals(EXTENSION_JPG);
        qualityLabel.setVisible(showJpgQuality);
        qualitySlider.setVisible(showJpgQuality);
        qualityValueLabel.setVisible(showJpgQuality);
        window.pack();
    }

    public static String getScreenshotFileName(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date d = new Date();
        String date = dateFormat.format(d);
        return "fractals_"+date;
    }

    public static void saveScreenshot(){
        FractalRenderer renderer = ((MainStage) FractalsGdxMain.stage).getFocusedRenderer();
        renderer.addScreenshotListener(new ScreenshotListener() {
            @Override
            public void madeScreenshot(byte[] data) {
                saveImage(data, getSingleScreenshotPath(), extensionSelect.getSelected(), null, renderer);
            }
        }, true);
        renderer.setSingleScreenshotScheduled(true);
    }

    static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    public static void recordScreenshots(String animationName){
        String screenshotFileName = getScreenshotFileName();
        FractalRenderer renderer = ((MainStage) FractalsGdxMain.stage).getFocusedRenderer();
        ParamAnimation animation = renderer.getRendererContext().getParamAnimation(animationName);
        if (animation == null)
            throw new IllegalArgumentException("Animation not found: "+animationName);
        recordScreenshots(animation, renderer, screenshotFileName);
    }

    public static void recordScreenshots(ParamAnimation animation, FractalRenderer renderer, String path){
        //TODO get animation and start, disable recording when done
        animation.setUsingFrameBasedProgress();
        animation.setPaused(true);
        animation.setFrameCounter(0);
//        PixmapIO.PNG pixmapIO = new PixmapIO.PNG();
//        pixmapIO.setCompression(7);
//        pixmapIO.setFlipY(false);
        ScreenshotListener screenshotListener = new ScreenshotListener() {
            @Override
            public void madeScreenshot(byte[] data) {
//                saveImage(data, getRecordingScreenshotPath(path+"\\", animation), extensionSelect.getSelected(), pixmapIO);
                String screenshotPath = getRecordingScreenshotPath("", path + "\\", animation);
                while (executor.getQueue().size() > 0){
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                executor.submit(() -> {
                    saveImage(data, screenshotPath, extensionSelect.getSelected(), null, renderer);
                    return null;
                });
            }
        };
        animation.addAnimationListener(new AnimationListener() {
            @Override
            public void animationFinished() {
                animation.setPaused(true);
                renderer.setScreenshotRecording(false);
                renderer.removeScreenshotListener(screenshotListener);
                animation.removeAnimationListener(this);
            }

            @Override
            public void animationProgressUpdated() {

            }
        });
        renderer.addScreenshotListener(screenshotListener, false);
        renderer.setScreenshotRecording(true);
        renderer.reset();
        animation.setPaused(false);
    }

    public static void previewScreenshot(){
        FractalRenderer renderer = ((MainStage) FractalsGdxMain.stage).getFocusedRenderer();
        renderer.addScreenshotListener(new ScreenshotListener() {
            @Override
            public void madeScreenshot(byte[] rawScreenshot) {
                byte[] data2 = getPreviewImageData(rawScreenshot, extensionSelect.getSelected());
                outputButton.setVisible(true);
                originalButton.setVisible(true);
//                imageSelect.setVisible(true);
                sizeLabel.setVisible(true);
                sizeLabel.setText("Size: "+data2.length/1000+" kb");

                if (compressedScreenshotPixmap != null)
                    compressedScreenshotPixmap.dispose();
                compressedScreenshotPixmap = new Pixmap(data2, 0, data2.length);
                updatePreviewImage();
                imageRenderer.setDimensions(640, 360);
                previewVisible = true;
                repopulateDynamicTable();
                window.pack();
                window.centerWindow();

                //System.out.println("preview image data length: "+data2.length/1000+" kb");
//                Pixmap pixmap2 = new Pixmap(data2, 0, data2.length);
            }
        }, true);
        renderer.setSingleScreenshotScheduled(true);
    }

    private static String getSingleScreenshotPath(){
        String folderText = folderTextField.getText();
        if (!folderText.endsWith("\\") && !folderText.endsWith("/"))
            folderText += "\\";
        return folderText + filenameTextField.getText() + extensionSelect.getSelected();
    }

    private static synchronized String getRecordingScreenshotPath(String prefix, ParamAnimation animation){
        int frame = animation.getFrameCounter();
        String frameFormatted = String.format("%0"+(int)Math.ceil(Math.log10(animation.getFrameCount()))+"d", frame);
        String folderText = folderTextField.getText();
        if (!folderText.endsWith("\\") && !folderText.endsWith("/"))
            folderText += "\\";
        return folderText + prefix + frameFormatted + extensionSelect.getSelected();
    }

    private static synchronized String getRecordingScreenshotPath(String prefix, String folder, ParamAnimation animation){
        int frame = animation.getFrameCounter();
        String frameFormatted = String.format("%0"+(int)Math.ceil(Math.log10(animation.getFrameCount()))+"d", frame);
        if (!folder.endsWith("\\") && !folder.endsWith("/"))
            folder += "\\";
        return folder + prefix + frameFormatted + extensionSelect.getSelected();
    }

    //Util

    public static void saveImage(byte[] pixels, String path, String extension, PixmapIO.PNG pixmapIO, FractalRenderer renderer){
        System.out.println("saving screenshot to "+path);
//        Pixmap pixmap = Pixmap.createFromFrameBuffer(0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        FileHandle fileHandle = Gdx.files.absolute(path);

        extension = extension.trim();
        if (extension.startsWith("."))
            extension = extension.substring(1);

        SystemContext systemContext = renderer.getSystemContext();
        String paramMetadataText = FractalsIOUtil.serializeParamContainers(systemContext.getParamContainer(), systemContext.getParamConfiguration(),
                FractalsGdxMain.mainStage.getClientParams(), FractalsGdxMain.mainStage.getClientParamConfiguration());
        try {
            if (extension.equalsIgnoreCase("png")) {

                //flip
                byte[] imgBytesFlipped = new byte[pixels.length];
                for (int y = 0 ; y < Gdx.graphics.getBackBufferHeight() ; y++){
                    for (int x = 0 ; x < Gdx.graphics.getBackBufferWidth() ; x++){
                        for (int channel = 0 ; channel < 4 ; channel++) {
                            imgBytesFlipped[channel + x*4 + Gdx.graphics.getBackBufferWidth()*4 * y] =
                                    pixels[channel + x*4 + Gdx.graphics.getBackBufferWidth()*4 * (Gdx.graphics.getBackBufferHeight() - 1 - y)];
                        }
                    }
                }
                Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
                BufferUtils.copy(imgBytesFlipped, 0, pixmap.getPixels(), pixels.length);

                //write with metadata
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (pixmapIO == null)
                    pixmapIO = new PixmapIO.PNG();
//                pixmapIO.setFlipY(true); //doesn't work?
                pixmapIO.write(baos, pixmap);

//                for (int i = 0 ; i < imgBytes.length ; i++){
//                    int x = i % pixmap.getWidth();
//                    int y = i / pixmap.getWidth();
//                    imgBytesFlipped[(pixmap.getHeight()-1-y)*pixmap.getWidth()+x] = imgBytes[i];
//                }
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                IIOMetadataUpdater.writeFileWithMetadata(bais, fileHandle.file(), METADATA_KEY, paramMetadataText);
//                pixmap.dispose();

//                if (pixmapIO != null) //buffers etc. reused
//                    pixmapIO.write(fileHandle, pixmap);
//                else
//                    PixmapIO.writePNG(fileHandle, pixmap);

            }
            else if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")) {
                fileHandle.parent().mkdirs();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MemoryCacheImageOutputStream os = new MemoryCacheImageOutputStream(baos);
                Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
                BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
                writeJpgWithQuality(os, pixmapToBufferedImage(pixmap, false), qualitySlider.getValue() / 100f);

                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                IIOMetadataUpdater.writeFileWithMetadata(bais, fileHandle.file(), METADATA_KEY, paramMetadataText);
                pixmap.dispose();
//                new FileImageOutputStream(fileHandle.file())

//                ImageIO.write(pixmapToBufferedImage(pixmap), "JPG", fileHandle.file()); //TODO Android support?

            } else
                throw new IllegalArgumentException("unsupported file type: "+extension+" supported are: png, jpg");

//            ImageMetadataHelper.copyFileWithMetadata(fileHandle.file().getAbsolutePath(), new HashMap<String, String>(){{put("parameters", "parameters go here...");}});
//            ImageMetadataHelper.writeCustomData(new BufferedIm)

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("saved screenshot to "+fileHandle.file().getAbsolutePath());
    }

    public static byte[] getPreviewImageData(byte[] pixels, String extension){
        if (originalScreenshotPixmap != null)
            originalScreenshotPixmap.dispose();
        originalScreenshotPixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
        BufferUtils.copy(pixels, 0, originalScreenshotPixmap.getPixels(), pixels.length);
        ByteArrayOutputStream encodedStream = new ByteArrayOutputStream();

        extension = extension.trim();
        if (extension.startsWith("."))
            extension = extension.substring(1);

        try {
            if (extension.equalsIgnoreCase("png"))
                writePng(new MemoryCacheImageOutputStream(encodedStream), pixmapToBufferedImage(originalScreenshotPixmap, false));
            else if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg"))
                writeJpgWithQuality(new MemoryCacheImageOutputStream(encodedStream), pixmapToBufferedImage(originalScreenshotPixmap, false), qualitySlider.getValue() / 100f);
            else
                throw new IllegalArgumentException("Extension not supported: "+extension+" supported extensions: png, jpg");
            byte[] data = encodedStream.toByteArray();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Source: https://stackoverflow.com/a/26319958
    public static void writeJpgWithQuality(ImageOutputStream outputStream, BufferedImage image, float qualityFactor) throws IOException{
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(qualityFactor);

//        ImageMetadataHelper

        jpgWriter.setOutput(outputStream);
        IIOImage outputImage = new IIOImage(image, null, null);
        jpgWriter.write(null, outputImage, jpgWriteParam);
        jpgWriter.dispose();
    }

    public static void writePng(ImageOutputStream outputStream, BufferedImage image) throws IOException{
        ImageWriter pngriter = ImageIO.getImageWritersByFormatName("png").next();

        pngriter.setOutput(outputStream);
        IIOImage outputImage = new IIOImage(image, null, null);
        pngriter.write(null, outputImage, null);
        pngriter.dispose();
    }


    //Source: https://www.badlogicgames.com/forum/viewtopic.php?f=11&t=8947&p=40600&hilit=saving+pixmap+as+jpeg#p40600
    public static BufferedImage pixmapToBufferedImage(Pixmap p, boolean flip) {
        int w = p.getWidth();
        int h = p.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int[] pixels = new int[w * h];
        for (int y=0; y<h; y++) {
            for (int x=0; x<w; x++) {
                //convert RGBA to RGB
                int value = p.getPixel(x, !flip ? y : h-y);
                int R = ((value & 0xff000000) >>> 24);
                int G = ((value & 0x00ff0000) >>> 16);
                int B = ((value & 0x0000ff00) >>> 8);

                int i = x + (y * w);
                pixels[ i ] = (R << 16) | (G << 8) | B;
            }
        }
        img.setRGB(0, 0, w, h, pixels, 0, w);
        return img;
    }
}
