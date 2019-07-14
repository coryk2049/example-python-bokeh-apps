FROM continuumio/anaconda3:latest

WORKDIR /home/anaconda/apps

COPY ./apps /home/anaconda/apps 

#RUN mkdir -p /home/anaconda/apps

#WORKDIR /home/anaconda/stocks
#CMD IP=$(hostname -i); /opt/conda/bin/bokeh serve --address 0.0.0.0 --allow-websocket-origin=${IP} .
#BOKEH_ALLOW_WS_ORIGIN=localhost:5006

#CMD /opt/conda/bin/bokeh serve --address 0.0.0.0:5006 --allow-websocket-origin=localhost:5006 /home/anaconda/apps/stocks

#CMD cd /home/anaconda/apps/ && /opt/conda/bin/bokeh serve stocks/ weather/ --allow-websocket-origin=localhost:5006

EXPOSE 5006

#CMD cd /home/anaconda/apps/ && /opt/conda/bin/bokeh serve --port 5006 stocks/ weather/ --allow-websocket-origin=localhost:5006 
#CMD cd /home/anaconda/apps/ && /opt/conda/bin/bokeh serve --port 5006 --allow-websocket-origin=localhost:5006 --prefix /stocks --prefix /weather

CMD /opt/conda/bin/bokeh serve --port 5006 $(find . -type d -mindepth 1 -maxdepth 1 -exec basename {} \;) --allow-websocket-origin=localhost:5006 
