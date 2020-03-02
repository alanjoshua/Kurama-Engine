# Rendering Engine

<p></p>
&nbsp &nbsp &nbsp

## Introduction

<p> This is a 3D rendering engine program.</p>

<p>This uses my linear algebra library for performing math operations </p>

<p>This project relies entirely on code written by my as I started this project around the end of December 2019 to learn how rendering engines work and using existing graphics APIs would defeat the point of me learning </p>

<p>Currently, the program runs entirely on the CPU and so the FPS would probably be really low on a relatively lower speced machine.</p>

<p> Since this project in its infancy and so far I have been mainly using it only to test different algorithms, it is not yet properly documented</p>

<p> This program currently does not have a properly functioning API as it embedded with my test program. I am working on making the graphics engine separate 

## Features

* Reading .OBJ files and rendering the models as a mesh
* Capable of triangulating n-gons (polygons with number of sides greater than 3)
* Switching between Matrix and Quaternion rotation mode
* Freely move camera's position and orientation with the keyboard and mouse/trackpad
* Ability to instantly change camera's position and orientation to focus on a particular model in the scene
* Ability to create basic GUI
* Input handling
* Option to use either perspective or orthographic projection
* Proper fullscreen support and UI elements scale with windows scaling, so things look good even in high DPI monitors

## Test Program
* The repository includes a test program named "RenderingEngine.jar" in the root folder. Download and run it to test it.
* The program has been tested on several windows 10 machines and one macbook air.  
* This program has a simple control scheme to control the camera using the keyboard and mouse.
* This program renders three 3D models on a grid.
* The purpose for creating this test program was for me to be able to test the features of my rendering engine.

### GUI controls
<p> The program takes control of the mouse to control the camera so press the escape key to toggle the pause screen and get access to normal mouse controls </p>

* Esc - Toggles the pause screen
* R - Currenly programmed to reorient camera to the first model in the scene (deer)
* Q - toggles between matrix and quaternion mode
* WASD - move camera position

### Currently known bugs

* Exit button is mistakenly clicked sometimes when nearby buttons are pressed 




