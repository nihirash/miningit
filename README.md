# MininGit

Simple manager for local git repositories.

**ANY contributions are welcome!**

## Usage

Basicly you need only JVM and SSH-server on your machine.

*Optionaly* you may create separate user(**better way**) for storing repositories. Via ```ssh-copy-id``` grant access to all developers.  

If you got precompiled version run ```java -jar miningit.jar --port <port-number>``` or just ```java -jar miningit.jar``` and configure application via web interface.

Or you may run it from sources with command:

```
lein run
```

Sample configs for systemd and apache2 are in folder ```config.samples```.

Feel free to contact me with any questions!  

## License

This software includes [min css framework](https://github.com/owenversteeg/min) thats licensed under MIT License.

MininGit

Copyright © 2018 Alexander Sharikhin

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
