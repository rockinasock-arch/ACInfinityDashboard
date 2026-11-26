import base64

m = (
    "PG1hbmlmZXN0IHhtbG5zOmFuZHJvaWQ9Imh0dHA6Ly9zY2hlbWFzLmFuZHJvaW"
    "QuY29tL2Fway9yZXMvYW5kcm9pZCI+PHVzZXMtcGVybWlzc2lvbiBhbmRyb2lk"
    "Om5hbWU9ImFuZHJvaWQucGVybWlzc2lvbi5JTlRFUk5FVCIvPjxhcHBsaWNhdG"
    "lvbiBhbmRyb2lkOmxhYmVsPSJBQyBJbmZpbml0eSIgYW5kcm9pZDp1c2VzQ2xl"
    "YXJ0ZXh0VHJhZmZpYz0idHJ1ZSIgYW5kcm9pZDp0aGVtZT0iQHN0eWxlL1RoZW"
    "1lLkFwcENvbXBhdC5Ob0FjdGlvbkJhciI+PGFjdGl2aXR5IGFuZHJvaWQ6bmFt"
    "ZT0iLk1haW5BY3Rpdml0eSIgYW5kcm9pZDpleHBvcnRlZD0idHJ1ZSIgYW5kcm"
    "9pZDpzY3JlZW5PcmllbnRhdGlvbj0ibGFuZHNjYXBlIj48aW50ZW50LWZpbHRl"
    "cj48YWN0aW9uIGFuZHJvaWQ6bmFtZT0iYW5kcm9pZC5pbnRlbnQuYWN0aW9uLk"
    "1BSU4iLz48Y2F0ZWdvcnkgYW5kcm9pZDpuYW1lPSJhbmRyb2lkLmludGVudC5j"
    "YXRlZ29yeS5MQVVOQ0hFUiIvPjwvaW50ZW50LWZpbHRlcj48L2FjdGl2aXR5Pj"
    "wvYXBwbGljYXRpb24+PC9tYW5pZmVzdD4="
)

open("app/src/main/AndroidManifest.xml", "wb").write(base64.b64decode(m))
