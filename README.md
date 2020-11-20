# Kurama Engine

<p> This project is a work-in-progress and so documentation is extremely lax right now. This is just something I am using to learn openGL/game engine design with, and so the API is messy at places and wasn't made with the intention of being a game engine to be used by other people (though feel free to mess around with it). Once I am satisfied with the features built into it, I'll work one cleaning up the API :) </p>
&nbsp &nbsp &nbsp

## Features
* OpenGL support
* Custom functional-style linear algebra library
* software rendering mode (Pure Java mode) if the user does not want to use openGL
* Reading .OBJ files and rendering the models as a mesh
* Support for directional, point and spot lights, with Phong Shading
* Directional and spotlight shadow mapping
* Random terrain generation
* Normal, diffuse and specular mapping
* Sky box
* Multiple materials for a single mesh (one draw call)
* Capable of triangulating n-gons (polygons with number of sides greater than 3) and other 3D modelling features
* Switching between Matrix and Quaternion rotation mode (Only in Pure Java Rendering Mode)
* Ability to create basic GUI (Buttons not yet implemented in openGL mode)
* Input handling
* Built-in simple benchmarking tool

<br>
<br>

![Image of a scene rendering by Kurama Engine](https://github.com/alanjoshua/Kurama-Engine/blob/master/images/java.exe%20Screenshot%202020.05.27%20-%2017.23.05.63.png)

## Test Program
* Download the demo from the releases tab. https://github.com/alanjoshua/RenderingEngine/releases



