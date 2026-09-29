import http.server
import socket
import socketserver
import os
import sys

PORT = 8080

def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Connect to an arbitrary external address to find primary network interface IP
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

class ApkHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        # Force download header for APK
        if self.path.endswith('.apk'):
            self.send_header('Content-Type', 'application/vnd.android.package-archive')
            self.send_header('Content-Disposition', 'attachment; filename="SpendTracker.apk"')
        super().end_headers()

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)
    apk_file = "SpendTracker.apk"

    if not os.path.exists(apk_file):
        print(f"[-] Error: {apk_file} not found in {script_dir}. Please build the app first.")
        sys.exit(1)

    ip = get_local_ip()
    url = f"http://{ip}:{PORT}/{apk_file}"

    print("=" * 65)
    print(" 🚀 SPENDTRACKER WI-FI DIRECT DOWNLOAD SERVER")
    print("=" * 65)
    print(f"\n Ensure your phone is connected to the same Wi-Fi network.")
    print(f"\n Open this link in Chrome on your phone to download the update:\n")
    print(f"   👉  {url}")
    print("\n" + "=" * 65)
    print(" Press Ctrl+C in this terminal when finished to stop the server.\n")

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), ApkHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")

if __name__ == '__main__':
    main()
