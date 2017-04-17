import sys
import matplotlib.pyplot as plt

# echo "backend : TkAgg" > ~/.matplotlib/matplotlibrc

def plot():
    nplots = len(sys.argv) - 1
    if (nplots not in [1,2]):
        print "Usage: draw.py <data_file> or draw.py <data_file1> <data_file2>"
        return -1
    maxsize = 0;
    maxtime = 0;
    for i in range(0, nplots) :
        size = []
        time = []


        f = open(sys.argv[i + 1])

        # get CP protocol name
        name = f.readline()[-4:-1]

        # save data into lists
        for line in f:
            line = line[:-1].split(" ")
            size.append(int(line[0]))
            time.append(float(line[1]))

        # plot
        plt.plot(size, time, label = name)

        # determine max size for plot bounds
        localmaxsize = max(size)
        if localmaxsize > maxsize:
            maxsize = localmaxsize

        # determine max time for plot bounds
        localmaxtime = max(time)
        if localmaxtime > maxtime:
            maxtime = localmaxtime

    plt.axis([1, maxsize, 0, maxtime])
    plt.xlabel("Size of file (bytes)")
    plt.ylabel("Time taken to transfer (s)")
    if nplots > 1: 
        plt.legend(loc='upper left')
        plt.savefig('output/combined.png', bbox_inches='tight')
    else:
        plt.savefig('output/' + name + '.png', bbox_inches='tight')

if __name__ == "__main__":
    plot()