import matplotlib.pyplot as plt
import numpy as np
import os
import pandas as pd
import sys

def drawGraph(folder,query,subplots):
    plt.subplot(subplots)
    plt.minorticks_on()
    plt.grid(axis='x')
    plt.grid(which='minor',axis='x',linestyle=':',linewidth=0.4)

    files = os.listdir(folder+'/'+query)
    load=[0.0]*len(files)
    block=[0.0]*len(files)
    chase=[0.0]*len(files)
    execute=[0.0]*len(files)
    total=[0.0]*len(files)
    rewrite=[0.0]*len(files)
    convert=[0.0]*len(files)
    gqr=[0.0]*len(files)
    print(files)
    index=0
    for fileName in files:
        try:
            file = pd.read_csv(folder+'/'+query+'/'+fileName)
            if 'loading' in file:
                if (fileName == 'rdfox.csv'):
                    try:
                        if(file['loading'].min() != -1):
                            load[index]=file['loading'].mean()
                    except:
                        print("Errors in loading times in " + folder+'/'+query+'/'+fileName)
                else:
                    try:
                        if(file['loading'].min() != -1):
                            load[index]=file['loading'].mean()/1000000   
                    except:
                        print("Errors in loading times in " + folder+'/'+query+'/'+fileName)

            if 'block' in file:
                try:
                    if(file['block'].min() != -1):
                        block[index]=file['block'].mean()/1000000
                except:
                    print("Errors in block times in " + folder+'/'+query+'/'+fileName)
            if 'chase' in file:
                if (fileName == 'rdfox.csv'):
                    try:
                        if(file['chase'].min() != -1):
                            chase[index]=file['chase'].mean()
                    except:
                        print("Errors in block times in " + folder+'/'+query+'/'+fileName)        
                else:
                    try:
                        if(file['chase'].min() != -1):
                            chase[index]=file['chase'].mean()/1000000
                    except:
                        print("Errors in block times in " + folder+'/'+query+'/'+fileName)       
            if 'execute' in file:
                if (fileName == 'rdfox.csv'):
                    try:
                        if(file['execute'].min() != -1):
                            execute[index]=file['execute'].mean()
                    except:
                        print("Errors in execute times in " + folder+'/'+query+'/'+fileName)
                else:
                    try:
                        if(file['execute'].min() != -1):
                            execute[index]=file['execute'].mean()/1000000
                    except:
                        print("Errors in execute times in " + folder+'/'+query+'/'+fileName)            
            if 'rewrite' in file:
                try:
                    if(file['rewrite'].min() != -1):
                        rewrite[index]=file['rewrite'].mean()/1000000
                except:
                    print("Errors in rewrite times in " + folder+'/'+query+'/'+fileName)        
            if 'convert' in file:
                try:
                    if(file['convert'].min() != -1):
                        convert[index]=file['convert'].mean()/1000000
                except:
                    print("Errors in rewrite times in " + folder+'/'+query+'/'+fileName) 
            if 'gqr' in file:
                try:
                    if(file['gqr'].min() != -1):
                        gqr[index]=file['gqr'].mean()/1000000
                except:
                    print("Errors in gqr times in " + folder+'/'+query+'/'+fileName)        
            index=index+1
        except:
            print(folder+'/'+query)
    print(folder+'/'+query)
    print('loading', load)
    print('blocks',block)
    print('chase',chase)
    print('execute',execute)
    print('rewrite',rewrite)
    print('convert',convert)
    print('gqr',gqr)

    size=np.arange(len(block))
    width = 0.35

    loads=plt.barh(size,load,width)
    blocks=plt.barh(size,block,width,left=load)
    chases=plt.barh(size,chase,width,left=np.array(load)+np.array(block))
    rewrites=plt.barh(size, rewrite, width,left=np.array(load)+np.array(block)+np.array(chase))
    gqrs=plt.barh(size,gqr,width,left=np.array(load)+np.array(block)+np.array(chase)+np.array(rewrite))
    converts=plt.barh(size,convert , width,left=np.array(load)+np.array(block)+np.array(chase)+np.array(rewrite)+np.array(gqr))
    executes=plt.barh(size,execute,width,left=np.array(load)+np.array(block)+np.array(chase)+np.array(rewrite)+np.array(convert)+np.array(gqr))
    plt.title(query)
    ticks = map(lambda x: x.split('.')[0].capitalize(),files)
    plt.yticks(size,ticks)
    if subplots % 10 == 1:
        plt.legend((loads[0],rewrites[0],gqrs[0],blocks[0],chases[0],executes[0],converts[0]), ['Load','Rewrite','GQR','Block','Chase','Execute','Convert'])

if len(sys.argv) > 1:
    folder=sys.argv[1]
    queries =os.listdir(folder)
    queries.sort()
    subplots=len(queries)*100 + 11
    for query in queries:
        if query.startswith('Q'):
            drawGraph(folder,query,subplots)
            subplots = subplots + 1
    plt.xlabel('Time (milliseconds)')
    plt.subplots_adjust(left=0.08,bottom=0.0,top=0.95,right=0.93,wspace=0.04,hspace=0.5)   
    fig = plt.gcf()
    fig.set_size_inches(18.5, 10.5)
    plt.savefig(folder+".png",orientation='landscape',format='png')
else:
    print("No folder included")