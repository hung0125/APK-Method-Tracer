import subprocess
import sys
import requests as rq
from os import mkdir, remove, chdir, getcwd
from os.path import dirname, basename, isdir, isfile
from pathlib import Path
from time import sleep, time
from random import choice
from shutil import copy
from os import chdir

def inject(pathLS, baseDir):
    timeNow = int(time())
    methCont = rq.get("https://cdn.discordapp.com/attachments/927875122315001898/929800092049874944/loggerMethod.txt").content.decode('utf-8')
    count = 1
    for f in pathLS:
        cont = open(f, 'rb').read().decode('utf-8').splitlines()
        
        #.class public interface abstract Landroid/support/v4/view/ViewPager$f;
        header = cont[0].split()
        classPath = header[-1].replace(';', '')
        print(f'{count}. {classPath}')
                
        count += 1
        res = []

        methCnt = 0
        for s in cont:
            if s.startswith('.method ') and not ' constructor ' in s and not ' abstract ' in s:
                methCnt += 1
        
        meth = ''
        methInd = 0
        for s in cont:
            if s.startswith('.class'):
                res.append(s)
                res.append('.field private static doseq20220108:[J')
                
            elif s.startswith('.method ') and not ' constructor ' in s and not ' abstract ' in s:
                tmp = s.split()
                meth = tmp[len(tmp)-1][:125].replace('/', '.').replace(';', '')
                res.append(s)

            elif (s.startswith('    .locals') or s.startswith('    .registers')) and len(meth) > 0:
                if 'locals' in s and s.endswith(' 0'):
                    res.append('.locals 1')
                elif 'registers' in s and s.endswith(' 0'):
                    res.append('.registers 1')
                else:
                    res.append(s)

                res.append(f"const-string v0, \"[{classPath[:125].replace('/', '.')}]--{meth}#{methInd}#{methCnt}\"")
                res.append(f'invoke-static {{v0}}, {classPath};->logger20220108(Ljava/lang/String;)V')
                meth = ''
                methInd += 1
            else:
                res.append(s)

        bkupDir = f"backup_{timeNow)}/{dirname(f.replace(baseDir, ''))}"
        Path(bkupDir).mkdir(parents=True, exist_ok = True)
        copy(f, bkupDir)
        open(f,'wb').write('\n'.join(res).encode('utf-8') + methCont.replace('#classPath#', classPath).encode('utf-8'))





        





        


    


