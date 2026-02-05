import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
import os


@pytest.fixture(scope="function")
def driver():
    options = UiAutomator2Options()
    options.platform_name = "Android"
    options.automation_name = "UiAutomator2"
    options.auto_grant_permissions = False  # We handle permissions manually

    # Check if running on AWS Device Farm
    if os.getenv("DEVICEFARM_DEVICE_UDID"):
        # Device Farm environment - app is pre-installed
        # Device Farm uses Appium 1.x which requires /wd/hub path
        options.app_package = "com.example.app"
        options.app_activity = ".MainActivity"
        options.no_reset = True
        server_url = "http://127.0.0.1:4723/wd/hub"
    else:
        # Local testing (Appium 2.x doesn't need /wd/hub)
        options.app_package = "com.example.app"
        options.app_activity = ".MainActivity"
        options.no_reset = False
        server_url = "http://127.0.0.1:4723"

    driver = webdriver.Remote(
        command_executor=server_url,
        options=options
    )
    driver.implicitly_wait(10)

    yield driver

    driver.quit()
