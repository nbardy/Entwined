package com.charlesgadeken.entwined;

import com.charlesgadeken.entwined.config.ConfigLoader;
import com.charlesgadeken.entwined.effects.EntwinedBaseEffect;
import com.charlesgadeken.entwined.model.Model;
import com.charlesgadeken.entwined.patterns.EntwinedBasePattern;
import com.charlesgadeken.entwined.triggers.http.AppServer;
import com.charlesgadeken.entwined.triggers.nfc.NFCEngine;
import heronarts.lx.LX;
import heronarts.lx.LXPlugin;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.studio.LXStudio;
import java.io.File;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.reflections.Reflections;
import processing.core.PApplet;

public class EntwinedGui extends PApplet implements LXPlugin {

    private static final String WINDOW_TITLE = "Entwined";

    private static int WIDTH = 1280;
    private static int HEIGHT = 800;
    private static boolean FULLSCREEN = false;

    Reflections reflections = new Reflections("com.charlesgadeken");
    private EngineController engineController;

    private LX lx;
    private Model model;
    private NFCEngine nfcEngine;
    final BooleanParameter[][] nfcToggles = new BooleanParameter[6][9];
    final BasicParameterProxy outputBrightness = new BasicParameterProxy(1);

    @Override
    public void settings() {
        if (FULLSCREEN) {
            fullScreen(PApplet.P3D);
        } else {
            size(WIDTH, HEIGHT, PApplet.P3D);
        }
    }

    @Override
    public void setup() {
        LXStudio.Flags flags = new LXStudio.Flags(this);
        flags.resizable = true;
        flags.useGLPointCloud = false;
        flags.startMultiThreaded = true;

        model = Model.fromConfigs();

        lx = new LXStudio(this, flags, model);
        this.surface.setTitle(WINDOW_TITLE);

        engineController = new EngineController(lx);

        if (ConfigLoader.enableNFC) {
            configureNFC(lx);
        }

        if (ConfigLoader.enableOutputBigtree) {
            // MRG TODO
            // lx.addEffect(new TurnOffDeadPixelsEffect(lx));
            Output.configureExternalOutput(lx, model, outputBrightness);
        }

        if (ConfigLoader.enableOutputMinitree) {
            Output.configureFadeCandyOutput(lx, outputBrightness);
        }

        System.out.println("setup() completed");
    }

    void configureServer() {
        new AppServer(lx, engineController).start();
    }

    void configureNFC(LX lx) {
        nfcEngine = new NFCEngine(lx);
        nfcEngine.start();

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 9; j++) {
                nfcToggles[i][j] = new BooleanParameter("toggle");
            }
        }

        nfcEngine.registerReaderPatternTypeRestrictions(
                Arrays.asList(readerPatternTypeRestrictions()));
        // this line to allow any nfc reader to read any cube
        nfcEngine.disableVisualTypeRestrictions = true;
    }

    VisualType[] readerPatternTypeRestrictions() {
        return new VisualType[] {
            VisualType.Pattern,
            VisualType.Pattern,
            VisualType.Pattern,
            VisualType.OneShot,
            VisualType.OneShot,
            VisualType.OneShot,
            VisualType.Effect,
            VisualType.Effect,
            VisualType.Effect,
            VisualType.Pattern,
        };
    }

    private void loadPatterns(LX lx) {
        reflections.getSubTypesOf(EntwinedBasePattern.class).forEach(lx.registry::addPattern);
    }

    private void loadEffects(LX lx) {
        reflections.getSubTypesOf(EntwinedBaseEffect.class).forEach(lx.registry::addEffect);
    }

    @Override
    public void initialize(LX lx) {
        // Here is where you should register any custom components or make modifications
        // to the LX engine or hierarchy. This is also used in headless mode, so note that
        // you cannot assume you are working with an LXStudio class or that any UI will be
        // available.

        // Register custom pattern and effect types

        loadPatterns(lx);
        loadEffects(lx);
    }

    public void initializeUI(LXStudio lx, LXStudio.UI ui) {
        // Here is where you may modify the initial settings of the UI before it is fully
        // built. Note that this will not be called in headless mode. Anything required
        // for headless mode should go in the raw initialize method above.
    }

    public void onUIReady(LXStudio lx, LXStudio.UI ui) {
        // At this point, the LX Studio application UI has been built. You may now add
        // additional views and components to the Ui heirarchy.
        //        lx.ui.preview.pointCloud.setPointSize(20);
    }

    @Override
    public void draw() {
        // All handled by core LX engine, do not modify, method exists only so that Processing
        // will run a draw-loop.
    }

    /**
     * Main interface into the program. Two modes are supported, if the --headless flag is supplied
     * then a raw CLI version of LX is used. If not, then we embed in a Processing 3 applet and run
     * as such.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        LX.log("Initializing LX version " + LXStudio.VERSION);
        boolean headless = false;
        File projectFile = null;
        for (int i = 0; i < args.length; ++i) {
            if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                System.out.println(
                        "java -jar path/to/entwined/jar SOMEPROJECT.lxp --flags\n"
                                + "Flags:\n"
                                + "\t--headless run in headless mode\n"
                                + "\t--fullscreen | -f : Run in fullscreen\n"
                                + "\t--width | -w : Set the width of the screen\n"
                                + "\t--height | -h : Set the height of the screen \n");
                System.exit(0);
            } else if ("--headless".equals(args[i])) {
                headless = true;
            } else if ("--fullscreen".equals(args[i]) || "-f".equals(args[i])) {
                FULLSCREEN = true;
            } else if ("--width".equals(args[i]) || "-w".equals(args[i])) {
                try {
                    WIDTH = Integer.parseInt(args[++i]);
                } catch (Exception x) {
                    LX.error("Width command-line argument must be followed by integer");
                }
            } else if ("--height".equals(args[i]) || "-h".equals(args[i])) {
                try {
                    HEIGHT = Integer.parseInt(args[++i]);
                } catch (Exception x) {
                    LX.error("Height command-line argument must be followed by integer");
                }
            } else if (args[i].endsWith(".lxp")) {
                try {
                    projectFile = new File(args[i]);
                } catch (Exception x) {
                    LX.error(x, "Command-line project file path invalid: " + args[i]);
                }
            }
        }
        if (headless) {
            // We're not actually going to run this as a PApplet, but we need to explicitly
            // construct and set the initialize callback so that any custom components
            // will be run
            headlessInit(projectFile);
        } else {
            PApplet.main(new String[] {EntwinedGui.class.getName()});
        }
    }

    public static LX.Flags headlessInit(@Nullable File projectFile) {
        LX.Flags flags = new LX.Flags();
        flags.initialize = new EntwinedGui();
        if (projectFile == null) {
            LX.log("WARNING: No project filename was specified for headless mode!");
        }
        LX.headless(flags, projectFile);
        return flags;
    }
}
