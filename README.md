# NightyNight Stories
This app offers children a fun storybook experience while improving their reading skills. Kids who read the text correctly are rewarded with magical animations.

<br>

<p align="center"><img src="https://github.com/user-attachments/assets/e30ce349-a540-4fa4-9aae-fe5fd914e138" style="width: 25%;"></p>

<br>

## Getting Started
Follow the steps below to set up and run the project on your local machine.

<br>

### 1. Prerequisites
- Android Studio
- API keys for integrating with Google Cloud Speech-to-Text API
- A mobile device or emulator running at least Android 6.0 (Marshmallow)

***Note:*** The images and videos used in the application are not included in this GitHub repository. You will need to create and add your own visuals and videos.

<br>

### 2. Installation
#### Clone the repository:
- ``` https://github.com/ErayBD/nightynight-stories.git ```
#### Build the project
- Open the project in Android Studio
- Update the Gradle configuration to install necessary dependencies:
  - ``` ./gradlew build ```

<br>

### 3. Configuration
#### Google Cloud Speech-to-Text API:
- Create a project in the Google Cloud Console
- Enable the Speech-to-Text API and create a service account.
- Copy the generated JSON key file to the app/src/main/res/raw/ directory.
- Update this line in "StoryBook.java":
  - ``` InputStream credentialsStream = getResources().openRawResource(R.raw.YOUR_JSON_FILENAME); ```
 
<br>

### 4. Running the Application
#### To run the application in Android Studio:
- ``` ./gradlew installDebug ```

<br>

### 5. Using the Application
#### Here’s how to use the application:
- ***Story Selection:*** When the app launches, the user is presented with three story choices. Currently, only the first story is active.
- ***Chapter Completion:*** To complete a chapter, the user must read the text correctly. Words that are read correctly turn green. Once the entire text is read correctly, a video replaces the static image, and the next chapter unlocks.
- ***Locked Chapters:*** Chapters that have not yet been completed are displayed with a blurry effect.

<br>

### 6. In-app Screenshots
<img src="https://github.com/user-attachments/assets/75ad2e95-20e4-4ea1-b0d2-9bc9c312138d" style="width: 33%;"/>

<br>

<img src="https://github.com/user-attachments/assets/612217f8-1c79-49cf-a462-87233fda6dd3" style="width: 33%;"/>

<br>

<img src="https://github.com/user-attachments/assets/9d2a0f99-df7c-4cf7-bc16-066c398ba495" style="width: 33%;"/>

<br>

<img src="https://github.com/user-attachments/assets/e8299331-a087-423c-8f4b-2241c3ef8563" style="width: 33%;"/>

<br>

### 6. Application Introduction Video GIF
![2024-09-01_22-36-29-ezgif com-crop](https://github.com/user-attachments/assets/be21f352-2b4b-47b3-addb-4adc4a99b557)

<br>

### 7. Application Introduction Video
<div align="left">
  <a href="https://www.youtube.com/shorts/iuPt7Q0OSqA" target="_blank"> <img src="https://github.com/user-attachments/assets/29176792-d1f9-473f-bfa3-5732536f10ed" alt="Video Title" style="width:50%;"></a>
</div>






