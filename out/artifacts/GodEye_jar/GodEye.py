#!/usr/bin/env python

''' Developer: Adriel Guerrero '''

import getopt
import re
import shodan
import socket
import subprocess
import sys
import time

version = '1.0'
limitCams = 0 ## arg2: [limitCams] (Limite de camaras a mostrar)
selectCam = 1 ## arg3 [selectCam] (Selecciona una camara por defecto de cada DVR)
newSearch = False

def usage():
	print 'Utilice: GodEye.py -l [limit] [--search]'
	print ''
	print 'argumentos opcionales:'
	print '-l, --limit\t limite de camaras a mostrar, use 0 para no usar limites'
	print '--search\t realizar una busqueda de nuevas direcciones IP'
	exit()
	
try:
	options, remainder = getopt.getopt(sys.argv[1:], 'l', ['limit=', 'search', 'help'])

	for opt, arg in options:
		if opt in ('-l', '--limit'):
			limitCams = arg
		elif (opt == '-s') | (opt == '--search'):
			newSearch = True
		elif opt == '--help':
			usage()
			sys.exit()
except getopt.GetoptError:
	print 'GodEye.py -l <limit> [--search]'
	sys.exit(2)

print '||========INICIANDO EL OJO DE DIOS========||'

SHODAN_API_KEY = "P6BRNtd8fXEBvDwFOmgW3v8WatrD2RlY"
api = shodan.Shodan(SHODAN_API_KEY)
outfileIPsName = 'ips.txt'
defaultPort = 554

time.sleep(3)

if newSearch == True:
	try:
		# Realizamos la busqueda en Shodan
		results = api.search('rtsp dahua', 1)
		print 'Realizando busqueda en Shodan...'
		time.sleep(2)
		
		# Guardar las IP's en un txt
		outfileIPs = None
		try:
			for result in results['matches']:
				outfileIPs = open(outfileIPsName, "a")
				ip = "".join(result['ip_str'])
				if ip is not '':
					outfileIPs.write(ip)
					outfileIPs.write("\n")
		except:
			print 'Ocurrio un error intentando guardar el archivo de IPs.'
		finally:
			outfileIPs.close()
	except shodan.APIError, e:
		if str(e) in 'Please upgrade your API plan to use filters or paging.':
			print 'Error: Necesitas actualizar tu plan en Shodan.'
		else:
			print 'Error al realizar consulta en Shodan: %s' % e
			exit()


	print 'Analizando direcciones IP...'
	time.sleep(1)

	# Leemos el nuevo archivo de salida
	IPs = []
	try:
		file = open(outfileIPsName, "r")
		for text in file.readlines(): # Todas las IPs las guardamos en una lista
			IPs.append(text.rstrip())
	except:
		print 'Ocurrio un error leyendo el nuevo archivo generado de direcciones IP.'
	finally:
		file.close()
		
	# Limpiar el archivo de las direcciones IP	
	try:
		# Remover IPs repetidas
		newIPs = set(IPs)
		
		# Comprobar que las IPs esten up y el puerto 554 abierto
		print 'Comprobando puertos abiertos...'
		CleanListIP = []
		for ip in newIPs:
			sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			sock.settimeout(5) # 5 segundos maximo
			result = sock.connect_ex((ip, defaultPort))
			sock.close()
			if result == 0: # Puerto abierto
				CleanListIP.append(ip)
				print 'Puerto abierto: %s' % ip
				
		# Ordenamos las IPs
		#CleanListIP.sort()

		# Reescribimos el archivo
		print 'Generando archivo de salida...'
		try:
			outfileNewIPs = open(outfileIPsName, "w")
			for newIP in CleanListIP:
				ip = "".join(newIP)
				if ip is not '':
					outfileNewIPs.write(ip)
					outfileNewIPs.write("\n")
		except:
			print 'Ocurrio un error intentando generar el archivo final de direcciones IP.'
		finally:
			outfileNewIPs.close()
	except Exception, e:
		print 'Ocurrio un error en el proceso de limpieza de las direcciones IP: %s' % e

print 'Ejecutando interfaz grafica...'
try:
	subprocess.call(['java', '-jar', 'GodEye.jar', str(limitCams)])
except:
	print '||========CERRANDO EL OJO DE DIOS========||'
	
