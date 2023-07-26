#### 3D Printed Insole with vibrator and button that uses bluetooth to communicate best moves through smartphone

___

## Project Screen Shots

![ALT](https://github.com/RamyPoe/ChessCheatInsole/blob/main/images/1.png?raw=true)
![ALT](https://github.com/RamyPoe/ChessCheatInsole/blob/main/images/2.jpg?raw=true)


## About

Uses ESP-32 for its bluetooth capabilities. A small motor with an offset weight controlled by a D882 transistor acts as the vibrator. A single button is used to relay your opponents moves via toe press. A single lipo battery with a lipo charger is used for easy recharging without opening the device.

The process involves first selecting whether you are playing black or white. If it is your opponents move, you tap the x and y coordinates of the where the piece was before, and where it is after. This move is translated to the phone which computes the best move and sends it back. The move is then vibrated back in the same manner (x and y).


## Credits

Chess engine: `https://github.com/sandermvdb/chess22k/tree/master`
