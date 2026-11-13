# Settings

The operation of PRISM can be configured in a number of ways. From the GUI, select "Options" from the main menu to bring up the "Options" dialog. The settings are grouped under several tabs. Those which affect the basic model checking functionality of the tool are under the heading "PRISM". Separate settings are available for the simulator and various aspects of the GUI (the model editor, the property editor and the log).

User options and settings for the GUI are saved in a file locally and reused. Currently the "Options" dialog in the GUI represents the easiest way to modify the settings, but the settings file is in a simple textual format and can also be edited by hand. To restore the default options for PRISM, click "Load Defaults" and then "Save Options" from the "Options" dialog in the GUI. Alternatively, delete the settings file re-launch the GUI. The location of the settings file depends on the operating system. As of PRISM 4.5, it is stored in:

- `$HOME/.prism` (if the settings file was already created by an older version of PRISM)
- `$XDG_CONFIG_HOME/prism.settings` (on Linux, if that environment variable is set)
- `$HOME/.config/prism.settings` (on Linux, if `$XDG_CONFIG_HOME` is not set)
- `$HOME/Library/Preferences/prism.settings` (on Mac OS)
- `.prism` in the user's home directory, e.g. `C:\Documents and Settings\username` (on Windows)

From the command-line version of PRISM, options are controlled by switches,
all of which are listed, with the details below, in "[Command-line switches](../Reference/CommandLineSwitches.md)".
A full list can also be displayed by typing:

```shell
prism -help
```

For some switches, whose format is not straightforward, there is additional help available on the command-line, using `-help switch`. For example:

```shell
prism -help const
prism -help simpath
prism -help exportresults
prism -help exportmodel
```

The settings file is *ignored* by the command-line version (unlike earlier versions of PRISM, where it was used). You can, however, request that the settings file *is* read, using the [`-settings`](../Reference/CommandLineSwitches.md#settings) switch, e.g.:

```shell
prism -settings ~/.prism
```

The other pages in this section describe the most important configuration options.
