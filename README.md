<br/>
<div id="simplifyQA-logo" align="center">
    <br />
    <img src=".theia\readmeImage.png" alt="SimplifyQA Logo" width="900" height="290"/>
    <h2>🚀 SimplifyQA Code Editor IDE</h2>
</div>

<div id="badges" align="center">
    <p>The <b>SimplifyQA Code Editor IDE</b> is designed for a seamless coding experience and integrates effortlessly with <b>SimplifyQA</b>. ✨</p>
    <p>It also features an <b>inbuilt AI 🤖</b> that assists with coding, making development smoother and more efficient.</p>
</div>

---

## 📝 How to Write the Code?
The **SimplifyQA Code-Editor** opens with a customized project tailored for you. Follow these steps to get started:

📂 **Project Navigation:**
- Navigate to the **project folder** and expand the structure.
- Go to **src/main/java/com/simplifyqa/codeeditor**.
- Refer to **SampleClass.java** for guidance.

🛠 **Coding Instructions:**
- Create **any number of packages and classes** inside `src/main/java/com/simplifyqa/codeeditor`.
- Use `@SyncAction` annotation to sync methods so they appear in the **SimplifyQA UI**.
- Maintain **unique IDs** for each custom method.
- Custom classes containing `@SyncAction` methods **must** have a public default constructor.
- Methods annotated with `@SyncAction` should be **public** and return a **boolean value**.

💡 **Driver Auto-Injection:**
Use the following annotations to automatically inject the driver during runtime:
- `@AutoInjectWebDriver` 🌐 (Web Automation)
- `@AutoInjectAndroidDriver` 🤖 (Android Automation)
- `@AutoInjectIOSDriver` 🍏 (iOS Automation)
- `@AutoInjectApiDriver` 🔄 (API Automation)
- `@AutoInjectMainFrameDriver` 🖥 (Mainframe Automation)

```java
@AutoInjectWebDriver
private IQAWebDriver driver;

@AutoInjectAndroidDriver
private IQAAndroidDriver androidDriver;
```

📌 **Additional Features:**
- `@AutoInjectCurrentObject` can be used to capture object attributes of the current step during test case recording.
```java
@AutoInjectCurrentObject
private SqaObject currentObject;
```
- Run **build.bat** (Windows) or **macBuild.sh** (Mac) to validate the code and build the JAR file.

📦 **JAR Execution Options:**
During build, you’ll see the prompt:
```
DO YOU WANT TO USE LOCAL JAR PRESENT IN TARGET FOLDER FOR EXECUTION? ENTER Y (for yes) OR N (for no):
```
- Enter **Y** to use the local JAR file in the `target` folder.
- Enter **N** to download and use the latest cloud JAR automatically.

⚠ **Build Validation:**
- If `@SyncAction` methods contain **duplicate unique IDs**, the build process will **fail** and display an error message showing:
  - **Unique ID**
  - **Methods**
  - **Classes** where duplication occurred.
```
SEVERE: ------------------DUPLICATE UNIQUE ID's are found------------------
 INFO:    UniqueId: MyProject-Sample-002
 methods: customSampleTypeText and customSampleTypeText
 classes: com.simplifyqa.codeeditor.SampleClass and class com.simplifyqa.codeeditor.innnerpack.SampleClass
```

🤖 **AI Assistance:**
- Use the inbuilt AI for **code suggestions** and **debugging**. It’s there to make your life easier! 🚀

---

## 👨‍💻 Developers
👤 **Abhishek M Balegundi** - [abhishek.m@simplify3x.com](mailto:abhishek.m@simplify3x.com)

---

## 🛠 Troubleshooting
⚡ **Best Practices:**
- Maintain **clean code** and eliminate syntax errors. ✅
- Contact **SimplifyQA Support Team** - [support@simplify3x.com](mailto:support@simplify3x.com) 📩
- Still confused about setting up the Code Editor? Reach out to **Abhishek M Balegundi** directly. 📞

---

Happy Coding! 🎉🚀

