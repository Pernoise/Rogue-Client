import sys
from pathlib import Path

from PyQt6.QtGui import QIcon
from PyQt6.QtWidgets import QApplication, QMainWindow

# This is a python script that should, in the future, install rogue's jar and java, and create symlinks, aliases, or links depending on the OS to rogue- so that the devs don't have to jpackage their way through.

resources = str(Path(__file__).parent.parent / "src/main/resources")
debug = True

class MainWindow(QMainWindow):
    def __init__(this):
        super().__init__()
        this.setWindowTitle("Rogue Alley")
        this.setWindowIcon(QIcon(resources + "/images/rogue-launch-inv.png"))
        this.setGeometry(300, 100, 1000, 500)
        if debug:
            print(resources + "/images/rogue-launch.png")
def main():
    app = QApplication(sys.argv)
    window = MainWindow()
    window.show()
    sys.exit(app.exec())
if __name__ == '__main__':
    main()
