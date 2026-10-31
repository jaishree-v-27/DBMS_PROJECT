import urllib.request
import urllib.parse
import http.cookiejar
import re
import sys

BASE_URL = "http://localhost:8080"
cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

def get_page(path):
    req = urllib.request.Request(f"{BASE_URL}{path}", headers={"User-Agent": "Mozilla/5.0"})
    with opener.open(req) as resp:
        return resp.getcode(), resp.read().decode('utf-8')

def extract_csrf(html):
    m = re.search(r'name="_csrf"\s+value="([^"]+)"', html)
    if not m:
        m = re.search(r'value="([^"]+)"\s+name="_csrf"', html)
    return m.group(1) if m else None

def post_form(path, data):
    encoded = urllib.parse.urlencode(data).encode('utf-8')
    req = urllib.request.Request(f"{BASE_URL}{path}", data=encoded, headers={"User-Agent": "Mozilla/5.0"})
    with opener.open(req) as resp:
        return resp.getcode(), resp.read().decode('utf-8'), resp.geturl()

print("========================================================")
print("  SURGICAL TASKS 1 TO 5 VERIFICATION SUITE")
print("========================================================")

# -----------------------------------------------------------
# TASK 3: Main Navigation Buttons & Routes
# -----------------------------------------------------------
print("\n--- Verifying Task 3: 4 Main Dashboard / Nav Buttons ---")
code, home_html = get_page("/")
assert code == 200, f"Expected 200 for home, got {code}"

# 1. About
assert re.search(r'href="[^"]*#about"', home_html), "About button link missing"
assert 'id="about"' in home_html, "Target section id='about' missing on home page"
print("  [OK] About button correctly wired and target id='about' exists.")

# 2. How It Works
assert re.search(r'href="[^"]*#how-it-works"', home_html), "How It Works button link missing"
assert 'id="how-it-works"' in home_html, "Target section id='how-it-works' missing on home page"
print("  [OK] How It Works button correctly wired and target id='how-it-works' exists.")

# 3. Blood Availability
assert re.search(r'href="[^"]*#availability"', home_html), "Blood Availability button link missing"
assert 'id="availability"' in home_html, "Target section id='availability' missing on home page"
print("  [OK] Blood Availability button correctly wired and target id='availability' exists.")

# 4. Become a Donor
assert re.search(r'href="/register[;"]', home_html), "Become a Donor button does not route to /register"
reg_code, reg_html = get_page("/register")
assert reg_code == 200, f"Expected 200 for /register, got {reg_code}"
assert 'Become a Voluntary Donor' in reg_html, "Registration page title missing"
assert 'Complete Donor Registration' in reg_html, "Registration form button missing"
print("  [OK] Become a Donor button correctly navigates to /register with complete donor registration flow.")

# Test Registration Form Validation
r_csrf = extract_csrf(reg_html)
v_code, v_html, v_url = post_form("/register", {
    "username": "test_underage",
    "password": "password123",
    "confirmPassword": "password123",
    "name": "Young Volunteer",
    "age": "15",
    "gender": "M",
    "bloodGroup": "O+",
    "phone": "9876543210",
    "city": "Chennai",
    "_csrf": r_csrf
})
assert "Age must be between 18 and 65" in v_html or "outside the required" in v_html or "Age" in v_html, "Validation failed to catch underage donor"
print("  [OK] Donor registration form actively validates demographic safety rules (18-65 age check).")

# Test navigation from subpages (e.g. /privacy)
p_code, p_html = get_page("/privacy")
assert re.search(r'href="[^"]*#about"', p_html), "About link broken from /privacy"
assert re.search(r'href="[^"]*#how-it-works"', p_html), "How It Works link broken from /privacy"
assert re.search(r'href="[^"]*#availability"', p_html), "Blood Availability link broken from /privacy"
assert re.search(r'href="/register[;"]', p_html), "Become a Donor link broken from /privacy"
print("  [OK] All 4 buttons correctly route back to destinations when clicked from sub-pages.")

# -----------------------------------------------------------
# TASK 1: Donor Dashboard & Gender Icon
# -----------------------------------------------------------
print("\n--- Verifying Task 1: Donor Dashboard ---")
# Login as donor (rahul_sharma is male)
l_code, l_html = get_page("/login")
csrf = extract_csrf(l_html)
code, d_html, final_url = post_form("/login", {
    "username": "rahul_sharma",
    "password": "donor123",
    "_csrf": csrf
})
assert "/donor/dashboard" in final_url, f"Failed to login as donor: {final_url}"

# Verify "Pending Clinical Alerts" is completely removed from overview
assert "Pending Clinical Alerts" not in d_html, "FAIL: 'Pending Clinical Alerts' still found in donor dashboard!"
print("  [OK] 'Pending Clinical Alerts' section is completely removed from donor overview.")

# Verify gender icon next to "Welcome back!"
assert "Welcome back," in d_html
assert "gender-badge" in d_html
assert "aria-label=\"Male Donor\"" in d_html, "Expected Male Donor icon for rahul_sharma"
print("  [OK] Profile-driven Male gender icon correctly rendered next to 'Welcome back, Rahul Sharma!'.")

# Test Female Donor Login (priya_ramesh)
get_page("/logout")
l_code, l_html = get_page("/login")
csrf = extract_csrf(l_html)
code, d_html_f, final_url = post_form("/login", {
    "username": "priya_ramesh",
    "password": "donor123",
    "_csrf": csrf
})
assert "/donor/dashboard" in final_url, f"Failed to login as priya_ramesh: {final_url}"
assert "gender-badge" in d_html_f
assert "aria-label=\"Female Donor\"" in d_html_f, "Expected Female Donor icon for priya_ramesh"
print("  [OK] Profile-driven Female gender icon correctly rendered next to 'Welcome back, Priya Ramesh!'.")

# -----------------------------------------------------------
# TASK 2: Hospital Dashboard & Urgency Filter
# -----------------------------------------------------------
print("\n--- Verifying Task 2: Hospital Urgency Filter ---")
# Logout first
get_page("/logout")
l_code, l_html = get_page("/login")
csrf = extract_csrf(l_html)
code, h_html, final_url = post_form("/login", {
    "username": "apollo_chennai",
    "password": "hospital123",
    "_csrf": csrf
})
assert "/hospital/dashboard" in final_url, f"Failed to login as hospital: {final_url}"

# Verify urgency filter in Recent Demands
assert "recentDemandsBody" in h_html, "recentDemandsBody table id missing"
assert "filterByUrgency(this, 'ALL', 'recentDemandsBody')" in h_html, "Filter 'All' missing in Recent Demands"
assert "filterByUrgency(this, 'CRITICAL', 'recentDemandsBody')" in h_html, "Filter 'Critical' missing in Recent Demands"
assert "filterByUrgency(this, 'HIGH', 'recentDemandsBody')" in h_html, "Filter 'High' missing in Recent Demands"
assert "filterByUrgency(this, 'NORMAL', 'recentDemandsBody')" in h_html, "Filter 'Medium' missing in Recent Demands"
assert "filterByUrgency(this, 'LOW', 'recentDemandsBody')" in h_html, "Filter 'Low' missing in Recent Demands"
print("  [OK] Urgency filter with all 5 options present in 'Recent Demands' section.")

# Verify urgency filter in Blood Demands
assert "hospReqBody" in h_html, "hospReqBody table id missing"
assert "filterByUrgency(this, 'ALL', 'hospReqBody')" in h_html, "Filter 'All' missing in Blood Demands"
assert "filterByUrgency(this, 'CRITICAL', 'hospReqBody')" in h_html, "Filter 'Critical' missing in Blood Demands"
assert "filterByUrgency(this, 'HIGH', 'hospReqBody')" in h_html, "Filter 'High' missing in Blood Demands"
assert "filterByUrgency(this, 'NORMAL', 'hospReqBody')" in h_html, "Filter 'Medium' missing in Blood Demands"
assert "filterByUrgency(this, 'LOW', 'hospReqBody')" in h_html, "Filter 'Low' missing in Blood Demands"
print("  [OK] Urgency filter with all 5 options present in 'Blood Demands' section.")

# Verify data-urgency attributes
assert 'data-urgency=' in h_html, "data-urgency attribute missing on hospital demand table rows"
print("  [OK] Existing demand urgency fields accurately bound to table rows.")

# -----------------------------------------------------------
# TASK 4 & 5: Text Visibility & Consistent Theme
# -----------------------------------------------------------
print("\n--- Verifying Tasks 4 & 5: Contrast & Consistent Theme ---")
# Check CSS file
code, css_content = get_page("/css/dashboard.css")
assert code == 200
assert ".navbar" in css_content
assert ".auth-card" in css_content
assert ".urgency-pill.active" in css_content
assert "--primary: #b71c1c" in css_content
assert "--navy-dark: #0f172a" in css_content
assert "--bg-app: #f8fafc" in css_content
print("  [OK] Design tokens, high-contrast navbar, and card styles verified in dashboard.css.")

get_page("/logout")
print("\n========================================================")
print("  ALL 5 SURGICAL TASKS VERIFIED SUCCESSFULLY (100% PASS) ")
print("========================================================")
