"""
Test SDK behavior with RANDOM permission acceptance/denial.
"""
import time
import random
from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import NoSuchElementException, InvalidSelectorException, WebDriverException


PERMISSION_ALLOW_BUTTON_IDS = [
    "com.android.permissioncontroller:id/permission_allow_button",
    "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
    "com.android.packageinstaller:id/permission_allow_button",
]

PERMISSION_ALLOW_TEXTS = [
    "Allow",
    "While using the app",
    "Only this time",
]

PERMISSION_DENY_BUTTON_IDS = [
    "com.android.permissioncontroller:id/permission_deny_button",
    "com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button",
    "com.android.packageinstaller:id/permission_deny_button",
]

PERMISSION_DENY_TEXTS = [
    "Deny",
    "Don't allow",
]


def handle_permission_randomly(driver):
    """Randomly grant or deny a permission dialog if present."""
    grant = random.choice([True, False])

    if grant:
        button_ids = PERMISSION_ALLOW_BUTTON_IDS
        button_texts = PERMISSION_ALLOW_TEXTS
        action = "GRANTED"
    else:
        button_ids = PERMISSION_DENY_BUTTON_IDS
        button_texts = PERMISSION_DENY_TEXTS
        action = "DENIED"

    # Try by ID first
    for button_id in button_ids:
        try:
            button = driver.find_element(AppiumBy.ID, button_id)
            button.click()
            print(f"Permission {action}")
            return True
        except NoSuchElementException:
            continue

    # Try by text (use double quotes in XPath to handle apostrophes like "Don't")
    for text in button_texts:
        try:
            button = driver.find_element(AppiumBy.XPATH, f'//*[@text="{text}"]')
            button.click()
            print(f"Permission {action}")
            return True
        except (NoSuchElementException, InvalidSelectorException, WebDriverException):
            continue

    return False


def handle_all_permissions_randomly(driver, max_attempts=10):
    """Handle all permission dialogs with random accept/deny."""
    for _ in range(max_attempts):
        time.sleep(1)
        if not handle_permission_randomly(driver):
            break
        time.sleep(0.5)


def test_sdk_with_random_permissions(driver):
    """
    Test SDK with random permission responses.
    """
    # Wait for the app to launch
    time.sleep(3)

    # Handle permissions randomly
    handle_all_permissions_randomly(driver)

    # Wait for SDK to initialize
    time.sleep(10)

    # App should still be running
    assert driver.current_activity is not None, "App crashed with random permissions"

    print("Test passed: SDK running with random permission responses")
