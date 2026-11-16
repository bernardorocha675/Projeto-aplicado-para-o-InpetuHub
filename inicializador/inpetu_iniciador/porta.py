"""Porta livre em 127.0.0.1 (o sistema nunca escuta na rede)."""
import socket


def livre():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]
