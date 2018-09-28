#!/bin/bash
echo "uninstalling npm packages required for testing..."
cd ~
sudo npm uninstall npm -g
sudo apt-get purge --auto-remove nodejs
sudo rm -rf ~/node_modules/
sudo rm ~/package-lock.json
sudo rm -rf /usr/lib/node_modules/
sudo rm -rf /usr/local/include/node_modules/
echo "npm uninstall done"