package com.simplifyqa.codeeditor.githandler;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Formatter;

public class GitRelease {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_BRIGHT_ORANGE = "\u001B[38;5;202m";

    private static final Logger log = Logger.getLogger(GitRelease.class.getName());

    static {
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                String level = record.getLevel().getLocalizedName();
                String message = record.getMessage().replaceAll("\\r?\\n", " ");
                if (level.equalsIgnoreCase("SEVERE")) {
                    return "[" + ANSI_RED + level + ANSI_RESET + "] " + message + System.lineSeparator();
                }
                return "[" + ANSI_BLUE + level + ANSI_RESET + "] " + message + System.lineSeparator();
            }
        });

        rootLogger.addHandler(consoleHandler);

        rootLogger.setLevel(Level.INFO);
    }

    public static void main(String[] args) {
        try {
            String versionReleasedString = executeRelease();
            log.info(ANSI_BRIGHT_ORANGE + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX> GIT RELEASE SUCCESSFULL: VERSION- "
                    + ANSI_YELLOW
                    + versionReleasedString + ANSI_BRIGHT_ORANGE + " <XXXXXXXXXXXXXXXXXXXXXXXXXXXXX" + ANSI_RESET);
            printSuccessMessage();
            System.exit(0);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.log(Level.SEVERE,
                    ANSI_RED + "FAILED TO PUBLISH THE JAR: " + ANSI_RESET + ANSI_YELLOW + e.getMessage() + ANSI_RESET);
            printErrorMessage();
            System.exit(1);
        }
    }

    private static String executeRelease() throws IOException, InterruptedException {
        String newVersion = "";
        Properties props = loadEnvFile();

        String gitUrl = props.getProperty("git.url");
        String gitUsername = props.getProperty("git.username");
        String gitToken = props.getProperty("git.token");

        String authUrl = gitUrl.replace("https://",
                "https://" + gitUsername.replace("@", "%40") + ":" + gitToken + "@");

        if (gitUrl.contains("bitbucket")) {
            String cleanUrl = gitUrl.replaceFirst("https://[^@]*@", "https://");
            authUrl = "https://" + gitUsername.replace("@", "%40") + ":" + gitToken + "@"
                    + cleanUrl.substring("https://".length());
        }

        Path target = Paths.get(System.getProperty("user.dir"), "target");
        Optional<Path> jar = Files.list(target)
                .filter(p -> p.toString().endsWith("jar-with-dependencies.jar"))
                .findFirst();
        if (jar.isEmpty())
            throw new RuntimeException("JAR file not found @: " + target);
        Path jarPath = jar.get();

        log.info("--- " + ANSI_GREEN + "JAR file found at: " + ANSI_RESET
                + jarPath);

        Path tempDir = Files.createTempDirectory("git-release");
        log.info("--- " + ANSI_GREEN + "Temporary directory created at: "
                + ANSI_RESET + tempDir);

        try {
            ProcessBuilder credentialPb = new ProcessBuilder("git", "config", "--global", "credential.helper", "store");
            credentialPb.redirectErrorStream(true);
            Process credentialProcess = credentialPb.start();
            waitFor(credentialProcess);

            ProcessBuilder branchPb = new ProcessBuilder("git", "ls-remote", "--heads", authUrl);
            branchPb.redirectErrorStream(true);
            Process branchProcess = branchPb.start();
            String branchOutput = new String(branchProcess.getInputStream().readAllBytes());
            waitFor(branchProcess);

            if (branchOutput.trim().isEmpty()) {
                log.info("[" + ANSI_YELLOW + "WARN" + ANSI_RESET + "]"
                        + "Repository is empty. Initializing with a README file..." + ANSI_RESET);

                ProcessBuilder initPb = new ProcessBuilder("git", "init");
                initPb.directory(tempDir.toFile());
                initPb.redirectErrorStream(true);
                Process initProcess = initPb.start();
                waitFor(initProcess);

                Path readmePath = tempDir.resolve("README.md");
                Files.write(readmePath, List.of("# My Project"), StandardOpenOption.CREATE);

                ProcessBuilder addReadmePb = new ProcessBuilder("git", "add", "README.md");
                addReadmePb.directory(tempDir.toFile());
                addReadmePb.redirectErrorStream(true);
                Process addReadmeProcess = addReadmePb.start();
                waitFor(addReadmeProcess);

                ProcessBuilder commitReadmePb = new ProcessBuilder("git", "commit", "-m", "Initial commit with README");
                commitReadmePb.directory(tempDir.toFile());
                commitReadmePb.redirectErrorStream(true);
                Process commitReadmeProcess = commitReadmePb.start();
                waitFor(commitReadmeProcess);

                String defaultBranch = "main";
                try {
                    ProcessBuilder defaultBranchPb = new ProcessBuilder("git", "symbolic-ref", "--short", "HEAD");
                    defaultBranchPb.directory(tempDir.toFile());
                    defaultBranchPb.redirectErrorStream(true);
                    Process defaultBranchProcess = defaultBranchPb.start();
                    defaultBranch = new String(defaultBranchProcess.getInputStream().readAllBytes()).trim();
                    waitFor(defaultBranchProcess);
                } catch (Exception e) {
                    log.log(Level.SEVERE,
                            ANSI_YELLOW + "Failed to determine default branch. Using 'main' as fallback." + ANSI_RESET);
                }

                ProcessBuilder pushPb = new ProcessBuilder("git", "push", authUrl, defaultBranch);
                pushPb.directory(tempDir.toFile());
                pushPb.redirectErrorStream(true);
                Process pushProcess = pushPb.start();
                waitFor(pushProcess);

                log.info("[" + ANSI_BLUE + "INFO" + ANSI_RESET + "]"
                        + "Repository initialized with a README file on branch: " + defaultBranch
                        + ANSI_RESET);
            }

            ProcessBuilder initPb = new ProcessBuilder("git", "init");
            initPb.directory(tempDir.toFile());
            initPb.redirectErrorStream(true);
            Process initProcess = initPb.start();
            waitFor(initProcess);

            Path tempJarPath = tempDir.resolve(jarPath.getFileName());
            Files.copy(jarPath, tempJarPath);

            ProcessBuilder addPb = new ProcessBuilder("git", "add", tempJarPath.getFileName().toString());
            addPb.directory(tempDir.toFile());
            addPb.redirectErrorStream(true);
            Process addProcess = addPb.start();
            waitFor(addProcess);

            ProcessBuilder commitPb = new ProcessBuilder("git", "commit", "-m", "Add JAR file for release");
            commitPb.directory(tempDir.toFile());
            commitPb.redirectErrorStream(true);
            Process commitProcess = commitPb.start();
            waitFor(commitProcess);

            ProcessBuilder fetchTagsPb = new ProcessBuilder("git", "ls-remote", "--tags", authUrl);
            fetchTagsPb.directory(tempDir.toFile());
            fetchTagsPb.redirectErrorStream(true);
            Process fetchTagsProcess = fetchTagsPb.start();
            String tagsOutput = new String(fetchTagsProcess.getInputStream().readAllBytes());
            waitFor(fetchTagsProcess);

            Pattern tagPattern = Pattern.compile("refs/tags/(V\\d+)");
            Matcher matcher = tagPattern.matcher(tagsOutput);
            List<String> tags = new ArrayList<>();
            while (matcher.find()) {
                tags.add(matcher.group(1));
            }
            Optional<Integer> latestVersion = tags.stream().map(s -> Integer.parseInt(s.split("V")[1])).distinct()
                    .sorted(Comparator.reverseOrder()).findFirst();

            int latestVersionNumber = latestVersion.orElse(0);
            latestVersionNumber = latestVersionNumber + 1;
            newVersion = "V" + latestVersionNumber;

            ProcessBuilder tagPb = new ProcessBuilder("git", "tag", "-a", newVersion, "-m", "Release " + newVersion);
            tagPb.directory(tempDir.toFile());
            tagPb.redirectErrorStream(true);
            Process tagProcess = tagPb.start();
            waitFor(tagProcess);

            ProcessBuilder pushTagPb = new ProcessBuilder("git", "push", authUrl, newVersion);
            pushTagPb.directory(tempDir.toFile());
            pushTagPb.redirectErrorStream(true);
            Process pushTagProcess = pushTagPb.start();
            waitFor(pushTagProcess);

            log.info("--- " + ANSI_GREEN + "JAR file pushed to tag: " + ANSI_YELLOW + newVersion + ANSI_RESET);
        } finally {
            deleteDirectory(tempDir);
            log.info("--- " + ANSI_GREEN + "Temporary directory deleted: " + ANSI_CYAN + tempDir + ANSI_RESET);
        }
        return newVersion;
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            FileUtils.deleteDirectory(path.toFile());
        }
    }

    private static void waitFor(Process process) throws IOException, InterruptedException {
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());

            String errorMessage = "Process failed with exit code " + exitCode + ".\n" +
                    "Command: " + String.join(" ", process.info().command().orElse("")) + "\n" +
                    "Output: " + output + "\n" +
                    "Error: " + error;

            throw new RuntimeException(errorMessage);
        }
    }

    private static String readStream(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString().trim();
        }
    }

    private static Properties loadEnvFile() throws IOException {
        Properties props = new Properties();
        Path envFilePath = Paths.get(".env");

        if (Files.exists(envFilePath)) {
            try (BufferedReader reader = Files.newBufferedReader(envFilePath)) {
                props.load(reader);
            }
        }

        int attempts = 0;
        final int MAX_ATTEMPTS = 3;

        while (attempts < MAX_ATTEMPTS) {
            if (isAnyValueMissing(props, "git.url", "git.username", "git.token")) {
                if (showSwingPopupAndUpdateEnvFile(props, envFilePath)) {
                    // User cancelled
                    System.exit(0);
                }
            }

            int validationResult = validateGitCredentials(props);
            switch (validationResult) {
                case 0: // Success
                    return props;
                case 1: // Repository issue
                    ConfirmPopup repoPopup = new ConfirmPopup(null,
                            "<html>Repository access problem.<br>Retry with different URL?</html>");
                    repoPopup.setVisible(true);
                    if (!repoPopup.isConfirmed()) System.exit(1);
                    else showSwingPopupAndUpdateEnvFile(props, envFilePath);
                    break;
                default: // Credential issue
                    ConfirmPopup credPopup = new ConfirmPopup(null,
                            "<html>Invalid credentials (attempt " + (attempts+1) + "/" + MAX_ATTEMPTS + ").<br>Try again?</html>");
                    credPopup.setVisible(true);
                    if (!credPopup.isConfirmed()) System.exit(1);
                    else showSwingPopupAndUpdateEnvFile(props, envFilePath);
                    attempts++;
            }

            // Refresh properties if user made changes
            try (BufferedReader reader = Files.newBufferedReader(envFilePath)) {
                props.load(reader);
            }
        }

        new InfoPopup(null, "Maximum attempts reached. Exiting.").setVisible(true);
        System.exit(1);
        return props; // Never reached
    }

    private static int validateGitCredentials(Properties props) {
        String gitUrl = props.getProperty("git.url");
        String gitUsername = props.getProperty("git.username");
        String gitToken = props.getProperty("git.token");

        String authUrl = gitUrl.replace("https://",
                "https://" + gitUsername.replace("@", "%40") + ":" + gitToken + "@");

        if (gitUrl.contains("bitbucket")) {
            String cleanUrl = gitUrl.replaceFirst("https://[^@]*@", "https://");
            authUrl = "https://" + gitUsername.replace("@", "%40") + ":" + gitToken + "@"
                    + cleanUrl.substring("https://".length());
        }

        try {
            ProcessBuilder branchPb = new ProcessBuilder("git", "ls-remote", "--heads", authUrl);
            branchPb.redirectErrorStream(true);
            Process branchProcess = branchPb.start();
            int exitCode = branchProcess.waitFor();

            if (exitCode == 0) {
                return 0;
            } else {
                String errorOutput = readStream(branchProcess.getInputStream());

                if (errorOutput.contains("Authentication failed")) {
                    log.log(Level.SEVERE, ANSI_RED + "Invalid Git credentials: " + ANSI_BRIGHT_ORANGE
                            + errorOutput + ANSI_RESET);
                    return -1;
                } else if (errorOutput.contains("Repository not found") ||
                        errorOutput.contains("Permission denied") ||
                        errorOutput.contains("Bad hostname") ||
                        errorOutput.contains("Port number")) {
                    log.log(Level.SEVERE, ANSI_RED + "Access denied: " + ANSI_BRIGHT_ORANGE + errorOutput + ANSI_RESET);
                    return 1;
                } else {
                    log.log(Level.SEVERE, ANSI_RED + "Error validating Git credentials: " + ANSI_BRIGHT_ORANGE
                            + errorOutput + ANSI_RESET);
                    return -2;
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.log(Level.SEVERE,
                    ANSI_RED + "Error validating Git credentials: " + ANSI_BRIGHT_ORANGE + e.getMessage() + ANSI_RESET);
            return -2;
        }
    }

    private static boolean isAnyValueMissing(Properties props, String... keys) {
        for (String key : keys) {
            String value = props.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean showSwingPopupAndUpdateEnvFile(Properties props, Path envFilePath) {
        GitCredentialsDialog dialog = new GitCredentialsDialog(null, props);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            String gitUrl = dialog.getGitUrl();
            String gitUsername = dialog.getGitUsername();
            String gitToken = dialog.getGitToken();

            if (!gitUrl.isEmpty() && !gitUsername.isEmpty() && !gitToken.isEmpty()) {
                props.setProperty("git.url", gitUrl);
                props.setProperty("git.username", gitUsername);
                props.setProperty("git.token", gitToken);

                try (BufferedWriter writer = Files.newBufferedWriter(envFilePath, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    props.store(writer, "Git Credentials");
                } catch (IOException e) {
                    new InfoPopup(null, "Failed to save .env file: " + e.getMessage()).setVisible(true);
                }
            } else {
                new InfoPopup(null, "All fields are required!").setVisible(true);
            }
        } else {
            new InfoPopup(null, "Operation cancelled. Exiting...").setVisible(true);
            return false;
        }
        return true;
    }

    private static void printSuccessMessage() {
        String successBanner = ANSI_GREEN +
                "\t\t\t\t                                                           \n" +
                "\t\t\t\t                                                           \n" +
                "\t\t\t\t  ███████╗██╗   ██╗ ██████╗ ██████╗███████╗███████╗███████╗\n" +
                "\t\t\t\t  ██╔════╝██║   ██║██╔════╝██╔════╝██╔════╝██╔════╝██╔════╝\n" +
                "\t\t\t\t  ███████╗██║   ██║██║     ██║     █████╗  ███████╗███████╗\n" +
                "\t\t\t\t  ╚════██║██║   ██║██║     ██║     ██╔══╝  ╚════██║╚════██║\n" +
                "\t\t\t\t  ███████║╚██████╔╝╚██████╗╚██████╗███████╗███████║███████║\n" +
                "\t\t\t\t  ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝╚══════╝╚══════╝╚══════╝\n" +
                "\t\t\t\t                                                           \n" +
                "\t\t\t\t                                                           \n" + ANSI_RESET;

        System.out.println(successBanner);
    }

    private static void printErrorMessage() {
        String errorBanner = ANSI_RED +
                "\t\t\t\t                                             \n" +
                "\t\t\t\t                                             \n" +
                "\t\t\t\t  ███████╗ █████╗ ██╗██╗     ███████╗██████╗ \n" +
                "\t\t\t\t  ██╔════╝██╔══██╗██║██║     ██╔════╝██╔══██╗\n" +
                "\t\t\t\t  █████╗  ███████║██║██║     █████╗  ██║  ██║\n" +
                "\t\t\t\t  ██╔══╝  ██╔══██║██║██║     ██╔══╝  ██║  ██║\n" +
                "\t\t\t\t  ██║     ██║  ██║██║███████╗███████╗██████╔╝\n" +
                "\t\t\t\t  ╚═╝     ╚═╝  ╚═╝╚═╝╚══════╝╚══════╝╚═════╝ \n" +
                "\t\t\t\t                                             \n" +
                "\t\t\t\t                                             \n" + ANSI_RESET;

        System.out.println(errorBanner);
    }
}