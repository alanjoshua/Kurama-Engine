# Kurama Engine

<p> This project is a work-in-progress and so documentation is extremely lax right now. This is just something I am using to learn openGL/game engine design with, and so the API is messy at places and wasn't made with the intention of being a game engine to be used by other people (though feel free to mess around with it). Once I am satisfied with the features built into it, I'll work one cleaning up the API :) </p>
&nbsp &nbsp &nbsp

## Features
* OpenGL support
* Custom functional-style linear algebra library
* Loading of models using assimp
* Modular rendering pipeline to allow users to stack and use multiple custom shaders with the engine. 
* Skeletal animation support
* A simple particle system
* Audio support using OpenAL
* Write scene to file and load it back. 
* Support for directional, point and spot lights, with Phong Shading
* Directional and spotlight shadow mapping
* Random terrain generation
* Normal, diffuse and specular mapping
* Sky box
* Multiple materials for a single mesh (one draw call)
* Capable of triangulating n-gons (polygons with number of sides greater than 3) and other 3D modelling features
* GUI systme using the new experimental Mesh shaders
* Supports a HUD overlay through the GUI system
* Support for text and create 3D meshes for text with any font in the system.
* Input handling

<br>
<br>

![Image of a scene rendering by Kurama Engine](https://github.com/alanjoshua/Kurama-Engine/blob/master/images/KuramaEngine-screenshot2.png)

<br>
<br>

### Project Board: https://github.com/users/alanjoshua/projects/1#card-50161872

<br>
<br>

## Test Program
* Download the demo from the releases tab. https://github.com/alanjoshua/RenderingEngine/releases  (Very outdated)

<br>
<br>

## IntelliJ IDE setup
* Ensure pom.xml is marked as a Maven file. If you don't see an 'M" symbol next to it, right click the file and select the option to mark it as a Maven file
* Ensure the JDK in project settings is Java 13. There are several places in the settings where you can set the JDK so ensure everything is Java 13. For some reason Java 13+ doesn't seem to work.
* In the Run configuration, ensure that RenderEngine (The inner Renderengine folder that contains the src, projects folder) is set as the working directory
* Mark the Editor folder as a source folder
* Mark the "java" folder in src/main as a source folder
* Mark the "code" folder inside all your projects inside the "projects" folder as a source folder



