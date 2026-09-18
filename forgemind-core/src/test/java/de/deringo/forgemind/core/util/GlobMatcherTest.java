package de.deringo.forgemind.core.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GlobMatcherTest {
    @Test
    void matchesRecursiveTestFile() {
        assertTrue(
                GlobMatcher.matches(
                        "*Test*.java",
                        "forgemind-core/src/test/java/foo/AgentTest.java"
                )
        );
    }

    @Test
    void matchesDoubleStar() {
        assertTrue(
                GlobMatcher.matches(
                        "**/pom.xml",
                        "forgemind-core/pom.xml"
                )
        );
    }

    @Test
    void doubleStarAlsoMatchesRoot() {
        assertTrue(
                GlobMatcher.matches(
                        "**/pom.xml",
                        "pom.xml"
                )
        );
    }

    @Test
    void singleStarDoesNotCrossDirectories() {
        assertFalse(
                GlobMatcher.matches(
                        "forgemind-core/*.java",
                        "forgemind-core/src/Agent.java"
                )
        );
    }

    @Test
    void questionMarkMatchesOneCharacter() {
        assertTrue(
                GlobMatcher.matches(
                        "Agent?.java",
                        "src/Agent1.java"
                )
        );
    }
}
