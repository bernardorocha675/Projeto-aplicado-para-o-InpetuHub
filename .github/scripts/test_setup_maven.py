"""Testes do download do Maven: falhas passageiras do Maven Central (404 e 5xx) são repetidas; o resto não."""
import importlib.util
import io
import pathlib
import unittest
import urllib.error
from unittest import mock

spec = importlib.util.spec_from_file_location('setup_maven', pathlib.Path(__file__).with_name('setup_maven.py'))
setup_maven = importlib.util.module_from_spec(spec)
spec.loader.exec_module(setup_maven)  # só define funções: nada é baixado ao importar


def erro_http(codigo):
    return urllib.error.HTTPError('https://exemplo.invalido/maven.zip', codigo, 'falha', {}, io.BytesIO(b''))


class Resposta(io.BytesIO):
    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False


class BaixarTest(unittest.TestCase):
    def setUp(self):
        self.esperas = []
        self.dormir = mock.patch.object(setup_maven.time, 'sleep', self.esperas.append)
        self.dormir.start()
        self.addCleanup(self.dormir.stop)

    def test_repete_404_e_502_passageiros(self):
        respostas = [erro_http(502), erro_http(404), Resposta(b'conteudo')]

        def abrir(url, timeout):
            r = respostas.pop(0)
            if isinstance(r, Exception):
                raise r
            return r

        with mock.patch.object(setup_maven.urllib.request, 'urlopen', abrir):
            self.assertEqual(setup_maven.baixar('https://exemplo.invalido/maven.zip', 30), b'conteudo')
        self.assertEqual(len(self.esperas), 2, 'esperou entre as tentativas')

    def test_repete_falha_de_conexao(self):
        respostas = [urllib.error.URLError('connection reset'), Resposta(b'ok')]

        def abrir(url, timeout):
            r = respostas.pop(0)
            if isinstance(r, Exception):
                raise r
            return r

        with mock.patch.object(setup_maven.urllib.request, 'urlopen', abrir):
            self.assertEqual(setup_maven.baixar('https://exemplo.invalido/maven.zip', 30), b'ok')

    def test_desiste_depois_de_tres_tentativas(self):
        chamadas = []

        def abrir(url, timeout):
            chamadas.append(url)
            raise erro_http(503)

        with mock.patch.object(setup_maven.urllib.request, 'urlopen', abrir):
            with self.assertRaises(urllib.error.HTTPError):
                setup_maven.baixar('https://exemplo.invalido/maven.zip', 30)
        self.assertEqual(len(chamadas), 3)

    def test_nao_repete_erro_que_nao_e_passageiro(self):
        chamadas = []

        def abrir(url, timeout):
            chamadas.append(url)
            raise erro_http(403)

        with mock.patch.object(setup_maven.urllib.request, 'urlopen', abrir):
            with self.assertRaises(urllib.error.HTTPError):
                setup_maven.baixar('https://exemplo.invalido/maven.zip', 30)
        self.assertEqual(len(chamadas), 1, '403 não melhora com nova tentativa')


if __name__ == '__main__':
    unittest.main()
