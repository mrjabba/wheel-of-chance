package com.cedarparkgames.wheelofchance;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class WheelGame extends ApplicationAdapter {
    static final float WIDTH = 1280, HEIGHT = 720, CX = 430, CY = 360, R = 300;
    static final float SPIN_SECONDS = 6;
    // values grouped by color: 0-19, 20-39, 40-59, 60-79, 80-100
    static final Color[] GROUPS = {
            Color.valueOf("ff5a5f"), Color.valueOf("ff9f43"), Color.valueOf("feca57"),
            Color.valueOf("1dd1a1"), Color.valueOf("54a0ff")};
    static final Color GOLD = Color.valueOf("d4af37"), BG = Color.valueOf("14121c"),
            SLICE_A = Color.valueOf("2a2440"), SLICE_B = Color.valueOf("1f1a30");

    private final Wheel wheel;
    private final Deque<Integer> history = new ArrayDeque<>();
    private final Matrix4 transform = new Matrix4();
    private FitViewport viewport;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font, titleFont, bigFont;
    private Sound tick;
    private float rotation, from, to, t = -1; // t < 0 means not spinning
    private int lastSlice;
    private Integer result;

    public WheelGame(Wheel wheel) {
        this.wheel = wheel;
    }

    static Color colorFor(int value) {
        return GROUPS[Math.min(value / 20, GROUPS.length - 1)];
    }

    @Override
    public void create() {
        viewport = new FitViewport(WIDTH, HEIGHT);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("Cinzel-Bold.ttf"));
        font = makeFont(gen, 34);
        titleFont = makeFont(gen, 40);
        bigFont = makeFont(gen, 72);
        gen.dispose();
        tick = makeTick();
        lastSlice = wheel.sliceAt(rotation);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                spin();
                return true;
            }
        });
    }

    private static BitmapFont makeFont(FreeTypeFontGenerator gen, int size) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size;
        p.minFilter = p.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        return gen.generateFont(p);
    }

    private void spin() {
        if (t >= 0) return;
        from = rotation;
        to = wheel.targetRotation(rotation, wheel.pickTarget());
        t = 0;
        result = null;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        update(Gdx.graphics.getDeltaTime());
        ScreenUtils.clear(BG);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);
        drawWheel();
        drawText();
    }

    private void update(float dt) {
        if (t < 0) return;
        t = Math.min(1, t + dt / SPIN_SECONDS);
        float eased = 1 - (float) Math.pow(1 - t, 3); // fast start, long slow finish
        rotation = from + (to - from) * eased;
        int slice = wheel.sliceAt(rotation);
        if (slice != lastSlice) {
            tick.play(0.6f);
            lastSlice = slice;
        }
        if (t >= 1) {
            t = -1;
            rotation %= 360;
            result = wheel.values[slice];
            history.addFirst(result);
            if (history.size() > 10) history.removeLast();
        }
    }

    private void drawWheel() {
        float w = Wheel.sliceDegrees();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(GOLD);
        shapes.circle(CX, CY, R + 14, 96);
        for (int i = 0; i < Wheel.SLICES; i++) { // thin colored band on the rim per value group
            shapes.setColor(colorFor(wheel.values[i]));
            shapes.arc(CX, CY, R, rotation + i * w, w, 24);
        }
        for (int i = 0; i < Wheel.SLICES; i++) { // cover the inside again, leaving only the band
            shapes.setColor(i % 2 == 0 ? SLICE_A : SLICE_B);
            shapes.arc(CX, CY, R - 12, rotation + i * w, w, 24);
        }
        shapes.setColor(GOLD);
        shapes.circle(CX, CY, 36, 48);
        shapes.setColor(BG);
        shapes.circle(CX, CY, 24, 48);
        // pointer at the top, aimed down at the wheel
        shapes.setColor(Color.WHITE);
        shapes.triangle(CX - 22, CY + R + 40, CX + 22, CY + R + 40, CX, CY + R - 22);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(GOLD);
        for (int i = 0; i < Wheel.SLICES; i++) {
            double a = Math.toRadians(rotation + i * w);
            shapes.line(CX, CY, CX + R * (float) Math.cos(a), CY + R * (float) Math.sin(a));
        }
        shapes.end();
    }

    private void drawText() {
        float w = Wheel.sliceDegrees();
        batch.begin();
        for (int i = 0; i < Wheel.SLICES; i++) {
            float mid = rotation + (i + 0.5f) * w;
            transform.idt().translate(CX, CY, 0).rotate(0, 0, 1, mid).translate(R * 0.72f, 0, 0).rotate(0, 0, 1, -90);
            batch.setTransformMatrix(transform);
            font.setColor(colorFor(wheel.values[i]));
            font.draw(batch, String.valueOf(wheel.values[i]), -60, font.getCapHeight() / 2, 120, Align.center, false);
        }
        batch.setTransformMatrix(transform.idt());

        float x = 850;
        titleFont.setColor(GOLD);
        titleFont.draw(batch, "Wheel Report", x, 650);
        if (result != null) {
            bigFont.setColor(colorFor(result));
            bigFont.draw(batch, String.valueOf(result), x, 580);
        } else {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, t >= 0 ? "Spinning..." : "Click to spin", x, 560);
        }
        font.setColor(Color.GRAY);
        font.draw(batch, "Last 10 spins", x, 470);
        float y = 420;
        int n = 1;
        for (int v : history) {
            font.setColor(Color.GRAY);
            font.draw(batch, n++ + ".", x, y);
            font.setColor(colorFor(v));
            font.draw(batch, String.valueOf(v), x + 70, y);
            y -= 38;
        }
        batch.end();
    }

    /** Short wooden-peg click, synthesized into a temp WAV so there's no sound asset to ship. */
    private static Sound makeTick() {
        int rate = 44100, n = rate / 30;
        ByteBuffer b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(36 + n * 2).put("WAVE".getBytes())
                .put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(rate).putInt(rate * 2).putShort((short) 2).putShort((short) 16)
                .put("data".getBytes()).putInt(n * 2);
        Random r = new Random(7);
        for (int i = 0; i < n; i++) {
            double env = Math.exp(-i / (rate * 0.006));
            double s = 0.6 * Math.sin(2 * Math.PI * 900 * i / rate) + 0.4 * (r.nextDouble() * 2 - 1);
            b.putShort((short) (s * env * 22000));
        }
        try {
            File f = File.createTempFile("wheel-tick", ".wav");
            f.deleteOnExit();
            FileHandle h = Gdx.files.absolute(f.getAbsolutePath());
            h.writeBytes(b.array(), false);
            return Gdx.audio.newSound(h);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        titleFont.dispose();
        bigFont.dispose();
        tick.dispose();
    }
}
