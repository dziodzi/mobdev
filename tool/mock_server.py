"""Local HTTP fixture. No external packages. Never use as production backend."""
import argparse, json, time, threading
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from pathlib import Path

lock = threading.Lock()
messages = {str(c): [dict(id=str(c*1000+i), **{"from":"demo", "data": {"Image":{"link":"sample"}} if i==43 else {"Text":{"text":f"Message {i}"}}}) for i in range(1, 46)] for c in (1,2)}
IMAGE = Path(__file__).with_name('sample.png').read_bytes()
class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args): pass  # Never log passwords or bearer tokens.
    def respond(self, code, payload, image=False):
        data = payload if image else json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", ("image/png" if data.startswith(b"\x89PNG") else "text/plain") if image else "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        try: self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError): pass
    def do_GET(self): self.handle_api()
    def do_POST(self): self.handle_api()
    def handle_api(self):
        global messages
        path = urlparse(self.path)
        parts = path.path.strip("/").split("/")
        time.sleep(args.delay)
        config = json.loads(Path(args.control).read_text()) if Path(args.control).exists() else {}
        if config.get("server_error"): return self.respond(503,{})
        if path.path == "/login" and self.command == "POST":
            body = json.loads(self.rfile.read(int(self.headers.get("Content-Length",0))))
            user = body.get("name")
            if user not in ("alice","bob") or body.get("pwd") != "password": return self.respond(401,{})
            return self.respond(200,("demo-"+user).encode(),True)
        token = self.headers.get("X-Auth-Token","")
        if config.get("unauthorized") or token not in ("demo-alice","demo-bob"): return self.respond(401,{})
        if path.path == "/logout": return self.respond(200,{})
        if path.path == "/channels": return self.respond(200,[] if config.get("empty") else ["1@channel","2@channel"])
        if len(parts)==2 and parts[0] in ("img","thumb"): return self.respond(200,IMAGE,True)
        if path.path=="/messages" or (len(parts)==2 and parts[0]=="channel"):
            channel = parts[1].replace("%40","@").split("@")[0] if len(parts)==2 else None
            if self.command=="POST":
                body=json.loads(self.rfile.read(int(self.headers.get("Content-Length",0))))
                channel=body["to"].split("@")[0]
                with lock:
                    m={"id":str(int(messages[channel][-1]["id"])+1),"from":body["from"],"data":body["data"]}
                    messages[channel].append(m)
                if config.get("lose_response"):
                    self.connection.shutdown(2); self.connection.close(); return
                return self.respond(200,m["id"].encode(),True)
            q=parse_qs(path.query)
            before=int(q.get("lastKnownId",["281474976710655"])[0])
            if config.get("page_error") and before<281474976710655: return self.respond(503,{})
            limit=min(20,max(1,int(q.get("limit",[20])[0])))
            with lock: eligible=sorted([m for m in messages[channel] if int(m["id"])<before],key=lambda m:int(m["id"]),reverse=True)
            page=eligible[:limit]
            return self.respond(200,page)
        self.respond(404,{})
if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--port",type=int,default=8080)
    parser.add_argument("--delay",type=float,default=0.5)
    parser.add_argument("--control",default="tool/mock_control.json")
    args=parser.parse_args()
    print(f"Mock listening on 0.0.0.0:{args.port}; alice/bob, password")
    ThreadingHTTPServer(("0.0.0.0",args.port),Handler).serve_forever()
