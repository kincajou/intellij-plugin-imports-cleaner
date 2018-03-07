# Intellij plugin: Java Imports Cleaner

Default IDEA mechanic of imports optimization changes the order of import statements. That leads to messy commit history in big projects with 
different code styles. This plugin replaces that mechanic, and, while keeping all the features, disables changing the order of import statements.
 
 ## Installation

 - make sure IDEA version is 2017.1 or higher / Android Studio 3.0.1 or higher  
 - download the jar file from [releases page](https://github.com/kincajou/intellij-plugin-imports-cleaner/releases)
 - open IDEA settings, Plugins tab, select 'Install plugin from disk...' and point it to the downloaded file
 
 The plugin starts working immediately. There is no configuration.
 
 ## Usage
 
 Once plugin is installed, default shortcut 'Ctrl+Alt+O' will work same as before except the order of imports will not be changed.
 
 Note that 'Optimize imports on the fly' option is not compatible with this plugin and uses default behavior that performs reorder. To achieve 
 automatic cleaning of unused imports (like i.e. Eclipse does), install [Save Actions](https://github.com/dubreuia/intellij-plugin-save-actions/) and 
 configure it to 'Organize imports'.
 
## Bugs / features

Any bug reports or suggestions should be made via github [issues](https://github.com/kincajou/intellij-plugin-imports-cleaner/issues). 
 
## Licence

[MIT License](LICENSE.txt)  
  
