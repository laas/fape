import random

def rabbit(area, num):
    return 'rab'+str(area)+'_'+str(num)
def area(num):
    return 'area'+str(num)
def agent(num):
    return 'bot'+str(num)
def rand(upper_bound):
    return random.randint(0, upper_bound-1)


hori = 1000
appear_uncertainty = 100
min_dur = 2
max_dur = 2


def gen(numAreas, numRabbitsPerArea, numAgents, numGoals, linked):
    def randArea():
        return area(random.randint(0,numAreas-1))
    def randRabbit():
        return rabbit(rand(numAreas), rand(numRabbitsPerArea))

    pbID = '%d-----%d' % (numAreas*numRabbitsPerArea, numGoals)
    f = None
    if linked:
        f = open('rabbits-linked.'+pbID+'.pb.anml', 'w')
    else:
        f = open('rabbits.'+pbID+'.pb.anml', 'w')
        
    for i in range(0,numAreas):
        f.write('\ninstance Area %s;\n' % area(i))
        for j in range(0,numRabbitsPerArea):
            f.write('instance Rabbit '+rabbit(i,j)+';\n')
            tp = 't_%d_%d' % (i, j)
            dur = random.randint(min_dur,max_dur)
            appearance_min = random.randint(0, hori)
            appearance_max = appearance_min + random.randint(0,appear_uncertainty)
            f.write('[%s] %s.at := %s;\n' % (tp, rabbit(i,j), area(i)))
            f.write('[%s+%d] %s.at := NONE;\n' % (tp,dur,rabbit(i,j)))
            if not linked or j == 0:
                f.write('%s :in start + [%d, %d];\n' % (tp, appearance_min, appearance_max))
            else:
                f.write('%s :in %s + [%d, %d];\n' % (tp, ('t_%d_%d'%(i,j-1)), appearance_min, appearance_max))
            
    f.write('\n\n')
    for i in range(0,numAgents):
        f.write('instance Agent '+agent(i)+';\n')
        f.write('[start] '+agent(i)+'.at := '+randArea()+';\n')

    f.write('\n')
    for i in range(0,numGoals):
        f.write('[end] %s.dead == true;\n' % randRabbit())


# gen(2,2,2,1,None)
# gen(3,4,4,2)

for areas in [2,3,4,6,8,10,12,16,20]:
    for density in [2,3,4,6,8,10]:
        for goals in [2,3,4,5,6,8,10,12]:
            gen(areas, density, 3, goals, False)
#            gen(areas, density, 3, goals, True)
