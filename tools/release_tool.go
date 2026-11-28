package main

import (
	"bufio"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
)

func main() {
	reader := bufio.NewReader(os.Stdin)

	// Determine project root
	var projectRoot string
	if len(os.Args) > 1 {
		projectRoot = os.Args[1]
	} else {
		projectRoot = findProjectRoot()
	}

	if projectRoot == "" {
		fmt.Print("📁 Enter path to Android project root: ")
		inputPath, _ := reader.ReadString('\n')
		projectRoot = strings.TrimSpace(inputPath)
	}

	absRoot, err := filepath.Abs(projectRoot)
	if err != nil || !isAndroidProject(absRoot) {
		fmt.Printf("❌ Error: '%s' is not a valid Android project root (missing app/build.gradle[.kts])\n", projectRoot)
		return
	}
	projectRoot = absRoot

	gradlePath := getGradleFilePath(projectRoot)

	fmt.Println("============================================")
	fmt.Println(" 🚀 Android CLI Release Manager (Golang)")
	fmt.Println("============================================")
	fmt.Printf("📂 Project Root: %s\n", projectRoot)

	for {
		code, name := readVersion(gradlePath)
		fmt.Printf("\n📋 Current Version -> Code: %d | Name: %s\n", code, name)
		fmt.Println("\nSelect an option:")
		fmt.Println("  1. Bump Patch & Code (+1)")
		fmt.Println("  2. Custom Version Bump")
		fmt.Println("  3. Git Status & Diffs")
		fmt.Println("  4. Git Commit & Tag")
		fmt.Println("  5. Build Release APK")
		fmt.Println("  6. Exit")
		fmt.Print("\nChoice: ")

		input, _ := reader.ReadString('\n')
		input = strings.TrimSpace(input)

		switch input {
		case "1":
			newCode := code + 1
			newName := incrementPatchName(name)
			updateVersion(gradlePath, newCode, newName)
		case "2":
			fmt.Print("Enter new versionCode (or press Enter to keep): ")
			cStr, _ := reader.ReadString('\n')
			cStr = strings.TrimSpace(cStr)
			newCode := code
			if cStr != "" {
				if parsed, err := strconv.Atoi(cStr); err == nil {
					newCode = parsed
				}
			}

			fmt.Printf("Enter new versionName (current: %s): ", name)
			newName, _ := reader.ReadString('\n')
			newName = strings.TrimSpace(newName)
			if newName == "" {
				newName = name
			}

			updateVersion(gradlePath, newCode, newName)
		case "3":
			runCommand(projectRoot, "git", "status")
			runCommand(projectRoot, "git", "diff", "app/build.gradle.kts")
		case "4":
			_, curName := readVersion(gradlePath)
			_, curCode := readVersion(gradlePath)
			runCommand(projectRoot, "git", "add", "app/build.gradle.kts")
			commitMsg := fmt.Sprintf("Release v%s (code %d)", curName, curCode)
			runCommand(projectRoot, "git", "commit", "-m", commitMsg)
			runCommand(projectRoot, "git", "tag", "v"+curName)
			fmt.Println("✅ Git commit and tag created successfully!")
		case "5":
			gradlew := "gradlew"
			if strings.Contains(strings.ToLower(os.Getenv("OS")), "win") {
				gradlew = "gradlew.bat"
			}
			gradlewPath := filepath.Join(projectRoot, gradlew)
			runCommand(projectRoot, gradlewPath, "assembleRelease")
		case "6":
			fmt.Println("Exiting. Goodbye!")
			return
		default:
			fmt.Println("❌ Invalid choice, please try again.")
		}
	}
}

func findProjectRoot() string {
	cwd, err := os.Getwd()
	if err != nil {
		return ""
	}

	dir := cwd
	for {
		if isAndroidProject(dir) {
			return dir
		}
		parent := filepath.Dir(dir)
		if parent == dir {
			break
		}
		dir = parent
	}

	// Fallback to parent of cwd (e.g. if running inside /tools)
	parent := filepath.Dir(cwd)
	if isAndroidProject(parent) {
		return parent
	}

	return ""
}

func isAndroidProject(dir string) bool {
	return getGradleFilePath(dir) != ""
}

func getGradleFilePath(dir string) string {
	f1 := filepath.Join(dir, "app", "build.gradle.kts")
	if _, err := os.Stat(f1); err == nil {
		return f1
	}
	f2 := filepath.Join(dir, "app", "build.gradle")
	if _, err := os.Stat(f2); err == nil {
		return f2
	}
	return ""
}

func readVersion(gradlePath string) (int, string) {
	contentBytes, err := os.ReadFile(gradlePath)
  if err != nil {
		return 0, "unknown"
	}
	content := string(contentBytes)

	code := 0
	name := "1.0.0"

	codeRegex := regexp.MustCompile(`versionCode\s*=\s*(\d+)`)
	if matches := codeRegex.FindStringSubmatch(content); len(matches) > 1 {
		code, _ = strconv.Atoi(matches[1])
	}

	nameRegex := regexp.MustCompile(`versionName\s*=\s*"([^"]+)"`)
	if matches := nameRegex.FindStringSubmatch(content); len(matches) > 1 {
		name = matches[1]
	}

	return code, name
}

func updateVersion(gradlePath string, newCode int, newName string) {
	contentBytes, err := os.ReadFile(gradlePath)
	if err != nil {
		fmt.Println("❌ Error reading gradle file:", err)
		return
	}
	content := string(contentBytes)

	codeRegex := regexp.MustCompile(`versionCode\s*=\s*\d+`)
	content = codeRegex.ReplaceAllString(content, fmt.Sprintf("versionCode = %d", newCode))

	nameRegex := regexp.MustCompile(`versionName\s*=\s*"[^"]+?"`)
	content = nameRegex.ReplaceAllString(content, fmt.Sprintf(`versionName = "%s"`, newName))

	err = os.WriteFile(gradlePath, []byte(content), 0644)
	if err != nil {
		fmt.Println("❌ Error writing gradle file:", err)
		return
	}
	fmt.Printf("💾 Updated build.gradle.kts -> versionCode = %d, versionName = \"%s\"\n", newCode, newName)
}

func incrementPatchName(name string) string {
	parts := strings.Split(name, ".")
	if len(parts) == 3 {
		if patch, err := strconv.Atoi(parts[2]); err == nil {
			parts[2] = strconv.Itoa(patch + 1)
			return fmt.Sprintf("%s.%s.%s", parts[0], parts[1], parts[2])
		}
	}
	return name
}

func runCommand(dir string, name string, arg ...string) {
	cmd := exec.Command(name, arg...)
	cmd.Dir = dir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	fmt.Printf("\n⚙️ Running: %s %s\n", name, strings.Join(arg, " "))
	err := cmd.Run()
	if err != nil {
		fmt.Printf("❌ Command failed: %v\n", err)
	}
}
