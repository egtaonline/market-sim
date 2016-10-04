
//
//  Message.h
//  itch4Parser
//
//  Created by Dylan Hurd on 11/3/13.
//  Copyright (c) 2013 Dylan Hurd. All rights reserved.
//

#ifndef __itch4Parser__Message__
#define __itch4Parser__Message__

#include <iostream>

using namespace std;

//Consts
// Length = (# of bytes/chars in spec)
const int TickerLength = 8;
const int MPIDLength = 4;
const int ReasonLength = 4;

//Standard TimeStamp

class TimeStamp {
protected:
  unsigned int seconds;
public:
  friend istream& operator>> (istream &input,  TimeStamp &ts);
  friend ostream& operator<< (ostream &output, TimeStamp &ts);
};

//
// Messages
//
class Message {
protected:
  unsigned int nanoseconds;
  char eventCode;
  
public:
  friend istream& operator>> (istream &input,  Message &ts);
  friend ostream& operator<< (ostream &output, Message &ts);
};

class StockDirectory {
protected:
  unsigned int nanoseconds;
  char ticker[TickerLength];
  char mktCategory;
  char finStatus;
  unsigned int roundLotSize;
  char roundLotStatus;
public:
  friend istream& operator>> (istream &input,  StockDirectory &ts);
  friend ostream& operator<< (ostream &output, StockDirectory &ts);
};

class StockTradingAction {
protected:
  unsigned int nanoseconds;
  char ticker[TickerLength];
  char tradingState;
  char reason[ReasonLength];
public:
  friend istream& operator>> (istream &input,  StockTradingAction &ts);
  friend ostream& operator<< (ostream &output, StockTradingAction &ts);
};

class ShortSalePriceTest {
private:
  unsigned int nanoseconds;
  char ticker[TickerLength];
  char regSHOAction;
public:
  friend istream& operator>> (istream &input,  ShortSalePriceTest &ts);
  friend ostream& operator<< (ostream &output, ShortSalePriceTest &ts);
};

class MarketParticipantPosition {
protected:
  unsigned int nanoseconds;
  char mpid[MPIDLength];
  char ticker[TickerLength];
  char mmStatus;
  char mmMode;
  char mpStatus;
public:
  friend istream& operator>> (istream &input,  MarketParticipantPosition &ts);
  friend ostream& operator<< (ostream &output, MarketParticipantPosition &ts);
};

class BrokenTrade {
protected:
  unsigned int nanoseconds;
  unsigned long matchNumber;
public:
  friend istream& operator>> (istream &input,  BrokenTrade &ts);
  friend ostream& operator<< (ostream &output, BrokenTrade &ts);
};

class NetOrderImbalance {
protected:
  unsigned int nanoseconds;
  unsigned long pairedShares;
  unsigned long imbalanceShares;
  char direction;
  char ticker[TickerLength];
  unsigned int farPrice;
  unsigned int nearPrice;
  unsigned int currentPrice;
  char crossType;
  char priceVar;
public:
  friend istream& operator>> (istream &input,  NetOrderImbalance &ts);
  friend ostream& operator<< (ostream &output, NetOrderImbalance &ts);
};

class RetailPriceImprovement {
protected:
  unsigned int nanoseconds;
  char ticker[TickerLength];
  char interest;
public:
  friend istream& operator>> (istream &input,  RetailPriceImprovement &ts);
  friend ostream& operator<< (ostream &output, RetailPriceImprovement &ts);
};

//
// Orders
//
class Order {
protected:
  unsigned int nanoseconds; //nanoseconds since last timestamp
  unsigned long refNum; //unique reference number
public:
};

class AddOrder : public Order {
protected:
  char buyStatus;
  unsigned int quantity;
  char ticker[TickerLength];
  unsigned int price;
  
public:
  friend istream& operator>> (istream &input,  AddOrder &order);
  friend ostream& operator<< (ostream &output, AddOrder &order);

};

class AddMPIDOrder : public AddOrder {
protected:
  char mpid[MPIDLength];
public:
  friend istream& operator>> (istream &input,  AddMPIDOrder &order);
  friend ostream& operator<< (ostream &output, AddMPIDOrder &order);
};

class ExecutedOrder : public Order {
protected:
  unsigned int quantity;
  unsigned long matchNumber;
public:
  friend istream& operator>> (istream &input,  ExecutedOrder &order);
  friend ostream& operator<< (ostream &output, ExecutedOrder &order);
};

class ExecutedPriceOrder : public ExecutedOrder {
protected:
  char printable;
  unsigned int price;
public:
  friend istream& operator>> (istream &input,  ExecutedPriceOrder &order);
  friend ostream& operator<< (ostream &output, ExecutedPriceOrder &order);
};

class CancelOrder : public Order{
protected:
  unsigned int quantity;
public:
  friend istream& operator>> (istream &input,  CancelOrder &order);
  friend ostream& operator<< (ostream &output, CancelOrder &order);
};


class DeleteOrder : public Order {
public:
  friend istream& operator>> (istream &input,  DeleteOrder &order);
  friend ostream& operator<< (ostream &output, DeleteOrder &order);
};

class ReplaceOrder : public Order {
protected:
  unsigned long oldRefNum;
  unsigned int quantity;
  unsigned int price;
  
public:
  friend istream& operator>> (istream &input,  ReplaceOrder &order);
  friend ostream& operator<< (ostream &output, ReplaceOrder &order);
};

class TradeMessage : public AddOrder {
protected:
  unsigned long matchNumber;
public:
  friend istream& operator>> (istream &input,  TradeMessage &order);
  friend ostream& operator<< (ostream &output, TradeMessage &order);
};

class CrossTradeMessage : public TradeMessage {
protected:
  unsigned long crossQuantity;
  char crossType;
public:
  friend istream& operator>> (istream &input,  CrossTradeMessage &order);
  friend ostream& operator<< (ostream &output, CrossTradeMessage &order);
};


#endif /* defined(__itch4Parser__Message__) */















