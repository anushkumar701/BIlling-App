const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 900,
    minHeight: 650,
    title: 'Fruit & Grocery POS — Desktop Terminal',
    backgroundColor: '#0a0f1d',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    },
    icon: path.join(__dirname, 'assets', 'icon.png')
  });

  // Load the POS UI (pointing to docs/index.html or local build)
  mainWindow.loadFile(path.join(__dirname, '..', 'docs', 'index.html'));

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

// Thermal print handler
ipcMain.handle('print-receipt', async (event, options) => {
  if (!mainWindow) return { success: false, error: 'No active window' };
  try {
    mainWindow.webContents.print({
      silent: true,
      printBackground: true,
      deviceName: options?.printerName || ''
    }, (success, failureReason) => {
      if (!success) console.error('Print failed:', failureReason);
    });
    return { success: true };
  } catch (err) {
    return { success: false, error: err.message };
  }
});
