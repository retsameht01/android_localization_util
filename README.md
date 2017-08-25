# android_localization_util

USAGE:
SETUP first create a csv file with the headers as the languages you want to localized.
each row after the header will be the translations for each locale.

Run the tool, it will prompt for a directory. Set the directory to be where your android res folder is located.
The tool will automatically pick up all the locales defined from the folders. Make sure to have the csv copy here as well.

Once a valid directory is entered the tool will ask for a row you want to use to enter data, type in the row.
***Note the tool will print out the data of the first column for each row. So ideally you want to format the csv to have the first column be
in the language of your choice.

Then simply type in the command update,['Name of your string id'] and hit Enter.
The tool will insert the localized string to the appropriate resource folder. You will also see the data showing on the console letting you know
which locale was inserted.

Repeat this as many time as you like for all the string id.
!!!Important make sure you set the dataRow (enter changeRow command) before you insert another string resource, otherwise it will insert the same data again to the new string resource.

Example usage:
/directory/for/android/res/folder
3 --- the data row to use
update,my_string_name -- use update instead of insert as it will add the xml tag for you if it's not there.

REPEAT these steps as many time as you want
changeRow --
2 --- set new row name
update,new_string_name --

type exit to end the tool



