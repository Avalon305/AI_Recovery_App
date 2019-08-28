package com.bdl.airecovery;

import com.bdl.airecovery.util.CodecUtils;

import org.junit.Test;

import java.math.BigInteger;
import java.text.DecimalFormat;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void myTest() {
        String a = ratingByResult(2);
        System.out.println(a);
        System.out.println(resultGrade(-10));
    }

    private String ratingByResult(int result) {
        double k = 5.0 / 60.0;
        double ratedResult = k * result + 3.33;
        ratedResult = (double) Math.round(ratedResult * 100) / 100;
        return String.valueOf(ratedResult);
    }

    private int resultGrade(int result) {
        if (result > 0 && result <= 20) {
            return 1;
        } else if (result > 20 && result <= 80) {
            return 2;
        } else if (result > 80 && result <= 140) {
            return 3;
        } else if (result > 140 && result <= 200) {
            return 4;
        } else if (result > 200) {
            return 5;
        }
        return 0;
    }


}