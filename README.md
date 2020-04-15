# Kurama Engine

<p></p>
&nbsp &nbsp &nbsp

## Features
* OpenGL support
* software rendering mode if the user does not want to use openGL
* Reading .OBJ files and rendering the models as a mesh
* Capable of triangulating n-gons (polygons with number of sides greater than 3)
* Switching between Matrix and Quaternion rotation mode
* Freely move camera's position and orientation with the keyboard and mouse/trackpad
* Ability to instantly change camera's position and orientation to focus on a particular model in the scene
* Ability to create basic GUI
* Input handling
* Option to use either perspective or orthographic projection
* Proper fullscreen support and UI elements scale with windows scaling, so things look good even in high DPI monitors
* Built-in simple benchmarking tool

## Test Program
* Download the demo from the releases tab. https://github.com/alanjoshua/RenderingEngine/releases

### GUI controls (for openGL renderer demo)
* v - switch between fullscreen and windowed modes
* ESC - toggle between camera movement and normal mouse controls
* R - focus on pre-selected model
* F - toggle between 165Hz FPS and uncapped FPS
* To quit the program, either switch to windowed mode, press ESC to get normal mouse control and close program using the normal close button at the top right. You can also close the program by pressing ALT-F4.

### GUI controls (for software renderer demo)
<p> The program takes control of the mouse to control the camera so press the escape key to toggle the pause screen and get access to normal mouse controls </p>

* Esc - Toggles the pause screen
* R - Currenly programmed to reorient camera to the first model in the scene (deer)
* Q - toggles between matrix and quaternion mode
* F - toggle frame rate cap (set at 165 frames currently)
* WASD - move camera position

### Currently known bugs

* Exit button is mistakenly clicked sometimes when nearby buttons are pressed 




