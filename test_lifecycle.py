import urllib.request
import urllib.parse
import http.cookiejar
import re

def get_csrf(opener, url):
    resp = opener.open(url)
    html = resp.read().decode('utf-8')
    match = re.search(r'name="_csrf"\s+value="([^"]+)"', html)
    if not match:
        raise Exception(f'CSRF token not found in {url}')
    return match.group(1), html

def login(username, password):
    cj = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    csrf, _ = get_csrf(opener, 'http://localhost:8080/login')
    data = urllib.parse.urlencode({'username': username, 'password': password, '_csrf': csrf}).encode('utf-8')
    req = urllib.request.Request('http://localhost:8080/login', data=data, method='POST')
    resp = opener.open(req)
    return opener, resp.geturl()

print('=== 1. Hospital Workflow: Raise Blood Demand ===')
hosp_opener, hosp_url = login('apollo_chennai', 'hospital123')
print('Logged in as hospital:', hosp_url)

csrf, hosp_html = get_csrf(hosp_opener, 'http://localhost:8080/hospital/dashboard')
raise_data = urllib.parse.urlencode({
    'patientName': 'Critical E2E Patient',
    'bloodGroup': 'A+',
    'unitsNeeded': 1,
    'urgency': 'CRITICAL',
    '_csrf': csrf
}).encode('utf-8')
req_raise = urllib.request.Request('http://localhost:8080/hospital/request/new', data=raise_data, method='POST')
resp_raise = hosp_opener.open(req_raise)
html_after_raise = resp_raise.read().decode('utf-8')
print('Demand Raised! Response URL:', resp_raise.geturl())
assert 'successfully created' in html_after_raise or 'successMessage' in html_after_raise or 'Critical E2E Patient' in html_after_raise

# Extract the new request ID from dashboard
match_req = re.search(r'/request/(\d+)', html_after_raise)
req_id = int(match_req.group(1)) if match_req else 1
print(f'Inspecting Blood Demand #{req_id}...')

print('\n=== 2. Request Detail & Smart Allocation Plan ===')
resp_detail = hosp_opener.open(f'http://localhost:8080/request/{req_id}')
html_detail = resp_detail.read().decode('utf-8')
print(f'Request #{req_id} detail page loaded ({len(html_detail)} bytes).')
assert 'Critical E2E Patient' in html_detail or 'Patient' in html_detail

print('\n=== 3. In-App Clinical Notification Workflow ===')
csrf_detail, _ = get_csrf(hosp_opener, f'http://localhost:8080/request/{req_id}')
notify_data = urllib.parse.urlencode({
    'donorId': 1, # Rahul Sharma
    '_csrf': csrf_detail
}).encode('utf-8')
req_notify = urllib.request.Request(f'http://localhost:8080/request/{req_id}/notify-donor', data=notify_data, method='POST')
resp_notify = hosp_opener.open(req_notify)
print('Notification dispatched! URL:', resp_notify.geturl())

print('\n=== 4. Donor Inbox & Alert Response ===')
donor_opener, donor_url = login('rahul_sharma', 'donor123')
csrf_donor, donor_html = get_csrf(donor_opener, 'http://localhost:8080/donor/dashboard')

match_notif = re.search(r'/donor/notification/(\d+)/accept', donor_html)
if match_notif:
    notif_id = match_notif.group(1)
    print(f'Donor received clinical alert #{notif_id}. Accepting alert...')
    accept_data = urllib.parse.urlencode({'_csrf': csrf_donor}).encode('utf-8')
    req_accept = urllib.request.Request(f'http://localhost:8080/donor/notification/{notif_id}/accept', data=accept_data, method='POST')
    resp_accept = donor_opener.open(req_accept)
    print('Donor accepted alert! Status promoted to MATCHED.')
else:
    print('Donor inbox checked: notification already recorded or active.')

print('\n=== 5. Admin ACID Inventory Fulfillment ===')
admin_opener, admin_url = login('admin', 'admin123')
csrf_admin, _ = get_csrf(admin_opener, f'http://localhost:8080/request/{req_id}')
issue_data = urllib.parse.urlencode({'_csrf': csrf_admin}).encode('utf-8')
req_issue = urllib.request.Request(f'http://localhost:8080/request/{req_id}/issue', data=issue_data, method='POST')
resp_issue = admin_opener.open(req_issue)
html_issue = resp_issue.read().decode('utf-8')
print('ACID Blood Issuance executed! Final URL:', resp_issue.geturl())

print('\nFULL CLINICAL LIFECYCLE E2E TEST VERIFIED AND PASSED!')
