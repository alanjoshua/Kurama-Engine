# Rendering Engine

<p></p>
&nbsp &nbsp &nbsp

## Introduction

<p> This is a 3D rendering engine program.</p>

<p>This uses my linear algebra library for performing math operations </p>

<p>This project relies entirely on code written by my as I started this project around the end of December 2019 to learn how rendering engines work and using existing graphics APIs would defeat the point of me learning </p>

<p>Currently, the program runs entirely on the CPU and so the FPS would probably be really low on a relatively lower speced machine.</p>

<p> Since this project in its infancy and so far I have been mainly using it only to test different algorithms, it is not yet properly documented</p>

## Features

* Reading .OBJ files and rendering the models as a mesh
* Capable of triangulating n-gons (polygons with number of sides greater than 3)
* Switching between Matrix and Quaternion rotation mode
* Freely move camera's position and orientation with the keyboard and mouse/trackpad
* Ability to instantly change camera's position and orientation to focus on a particular model

## Usage

* The repository has a test program with a main class. Run it to test the program
* The test program has only been tested on my windows laptop and PC and would probably not work on a Mac as there is a difference between handling file path and windows management.

### GUI controls
<p> The program takes control of the mouse to control the camera so press the escape key on your keyboard to switch to normal mouse movement mode</p>

* Esc - Toggles between mouse camera control and normal desktop control mode
* R - Currenly programmed to reorient camera to the first model in the scene (deer)
* Q - toggles between matrix and quaternion mode
* WASD - move camera position






