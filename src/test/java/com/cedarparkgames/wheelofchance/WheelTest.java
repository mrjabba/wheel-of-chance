package com.cedarparkgames.wheelofchance;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class WheelTest {
    @Test
    void landsOnPickedSlice_andRiggedOnlyHitsGivenValues() {
        Wheel wheel = new Wheel(new Random(42), Wheel.parse("10, 70"));
        float rotation = 0;
        for (int n = 0; n < 1000; n++) {
            int target = wheel.pickTarget();
            float next = wheel.targetRotation(rotation, target);
            assertTrue(next - rotation >= 360 * 5);
            assertEquals(target, wheel.sliceAt(next));
            int v = wheel.values[target];
            assertTrue(v == 10 || v == 70, "got " + v);
            rotation = next;
        }
    }

    @Test
    void fairWheelValuesInRange() {
        Wheel wheel = new Wheel(new Random(1), List.of());
        for (int v : wheel.values) assertTrue(v >= 0 && v <= 100);
    }
}
